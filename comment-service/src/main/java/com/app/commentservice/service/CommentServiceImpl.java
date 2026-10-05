package com.app.commentservice.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.app.commentservice.dto.CommentCreateRequest;
import com.app.commentservice.dto.CommentResponse;
import com.app.commentservice.dto.CommentUpdateRequest;
import com.app.commentservice.dto.ModerationModeRequest;
import com.app.commentservice.entity.AppUser;
import com.app.commentservice.entity.Comment;
import com.app.commentservice.entity.CommentLike;
import com.app.commentservice.entity.CommentStatus;
import com.app.commentservice.messaging.NotificationDispatchEvent;
import com.app.commentservice.repository.AppUserRepository;
import com.app.commentservice.repository.CommentLikeRepository;
import com.app.commentservice.repository.CommentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentServiceImpl.class);
    private static final Pattern MENTION_PATTERN = Pattern.compile("(?<![\\w.])@([A-Za-z0-9._-]{3,80})");

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final AppUserRepository appUserRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectProvider<KafkaTemplate<String, NotificationDispatchEvent>> notificationKafkaTemplateProvider;

    @Value("${inkwell.comment.moderation-required:false}")
    private boolean moderationRequired;

    @Value("${inkwell.post-service-url:http://localhost:8082}")
    private String postServiceUrl;

    @Value("${inkwell.notification.kafka.topic:notification.dispatch.v1}")
    private String notificationKafkaTopic;

    @Value("${spring.application.name:comment-service}")
    private String applicationName;

    private volatile boolean moderationModeInitialized;
    private volatile boolean moderationMode;

    @Override
    @Transactional
    public CommentResponse addComment(CommentCreateRequest request) {
        initializeModerationModeIfNeeded();
        Comment parent = null;
        if (request.getParentCommentId() != null) {
            parent = findComment(request.getParentCommentId());
            if (!parent.getPostId().equals(request.getPostId())) {
                throw new IllegalArgumentException("Parent comment does not belong to this post");
            }
            if (parent.getParentCommentId() != null) {
                throw new IllegalArgumentException("Only two-level threading is supported");
            }
            if (parent.getStatus() == CommentStatus.DELETED) {
                throw new IllegalArgumentException("Cannot reply to deleted comment");
            }
        }

        Comment comment = Comment.builder()
                .postId(request.getPostId())
                .authorId(request.getAuthorId())
                .authorName(resolveAuthorName(request.getAuthorName(), request.getAuthorId()))
                .parentCommentId(request.getParentCommentId())
                .content(request.getContent().trim())
                .status(moderationMode ? CommentStatus.PENDING : CommentStatus.APPROVED)
                .build();
        comment = commentRepository.save(comment);

        Long actorId = comment.getAuthorId();
        String actorName = resolveAuthorName(comment.getAuthorName(), actorId);
        Set<Long> alreadyNotified = new LinkedHashSet<>();
        // Notify post author immediately on new comment creation, even if moderation is enabled.
        notifyPostAuthorIfNeeded(comment, actorId, actorName, alreadyNotified);

        if (comment.getStatus() == CommentStatus.APPROVED) {
            dispatchReplyAndMentionNotifications(comment, parent, alreadyNotified);
        }
        return toResponse(comment);
    }

    @Override
    public List<CommentResponse> getCommentsByPost(Long postId) {
        return commentRepository.findByPostId(postId).stream()
                .filter(comment -> comment.getStatus() != CommentStatus.DELETED)
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<CommentResponse> getAllComments(CommentStatus status) {
        if (status == CommentStatus.DELETED) {
            return List.of();
        }
        List<Comment> rows = status == null ? commentRepository.findAll() : commentRepository.findByStatus(status);
        return rows.stream()
                .filter(comment -> comment.getStatus() != CommentStatus.DELETED)
                .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CommentResponse getCommentById(Long commentId) {
        Comment comment = findComment(commentId);
        if (comment.getStatus() == CommentStatus.DELETED) {
            throw new IllegalArgumentException("Comment not found");
        }
        return toResponse(comment);
    }

    @Override
    public List<CommentResponse> getReplies(Long commentId) {
        findComment(commentId);
        return commentRepository.findByParentCommentId(commentId).stream()
                .filter(comment -> comment.getStatus() != CommentStatus.DELETED)
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CommentResponse updateComment(Long commentId, CommentUpdateRequest request) {
        Comment comment = findComment(commentId);
        if (!comment.getAuthorId().equals(request.getAuthorId())) {
            throw new IllegalArgumentException("You can only edit your own comment");
        }
        if (comment.getStatus() == CommentStatus.DELETED) {
            throw new IllegalArgumentException("Deleted comment cannot be edited");
        }
        long minutes = Duration.between(comment.getCreatedAt(), LocalDateTime.now()).toMinutes();
        if (minutes > 30) {
            throw new IllegalArgumentException("Comment edit window has expired");
        }
        comment.setContent(request.getContent().trim());
        comment = commentRepository.save(comment);
        return toResponse(comment);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long actorId, String actorRole) {
        if (actorId == null) {
            throw new IllegalArgumentException("actorId is required");
        }
        Comment comment = findComment(commentId);
        boolean isAdmin = "ADMIN".equalsIgnoreCase(actorRole == null ? "" : actorRole);
        boolean isCommentOwner = comment.getAuthorId().equals(actorId);
        boolean isPostOwner = isPostOwner(comment.getPostId(), actorId);
        if (!isAdmin && !isCommentOwner && !isPostOwner) {
            throw new IllegalArgumentException("You are not allowed to delete this comment");
        }
        Set<Long> deleteIds = new LinkedHashSet<>();
        deleteIds.add(comment.getCommentId());
        if (comment.getParentCommentId() == null) {
            List<Comment> replies = commentRepository.findByParentCommentId(comment.getCommentId());
            replies.stream().map(Comment::getCommentId).forEach(deleteIds::add);
        }
        commentLikeRepository.deleteByCommentIdIn(deleteIds);
        commentRepository.deleteByCommentIdIn(deleteIds);
    }

    @Override
    @Transactional
    public void deleteCommentsByPost(Long postId) {
        List<Comment> comments = commentRepository.findByPostId(postId);
        if (comments.isEmpty()) {
            return;
        }
        List<Long> commentIds = comments.stream().map(Comment::getCommentId).toList();
        commentLikeRepository.deleteByCommentIdIn(commentIds);
        commentRepository.deleteByPostId(postId);
    }

    @Override
    @Transactional
    public CommentResponse approveComment(Long commentId, Long actorId, String actorRole) {
        Comment comment = findComment(commentId);
        ensureModerationPermission(comment, actorId, actorRole);
        CommentStatus previousStatus = comment.getStatus();
        comment.setStatus(CommentStatus.APPROVED);
        comment = commentRepository.save(comment);
        if (previousStatus != CommentStatus.APPROVED) {
            Comment parent = comment.getParentCommentId() == null ? null : findComment(comment.getParentCommentId());
            dispatchReplyAndMentionNotifications(comment, parent, new LinkedHashSet<>());
        }
        return toResponse(comment);
    }

    @Override
    @Transactional
    public CommentResponse rejectComment(Long commentId, Long actorId, String actorRole) {
        Comment comment = findComment(commentId);
        ensureModerationPermission(comment, actorId, actorRole);
        comment.setStatus(CommentStatus.REJECTED);
        comment = commentRepository.save(comment);
        return toResponse(comment);
    }

    @Override
    @Transactional
    public void likeComment(Long commentId, Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        Comment comment = findComment(commentId);
        if (comment.getStatus() == CommentStatus.DELETED) {
            throw new IllegalArgumentException("Comment is deleted");
        }
        if (commentLikeRepository.findByCommentIdAndUserId(commentId, userId).isPresent()) {
            return;
        }
        commentLikeRepository.save(CommentLike.builder().commentId(commentId).userId(userId).build());
        commentRepository.incrementLikes(commentId);
    }

    @Override
    @Transactional
    public void unlikeComment(Long commentId, Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        findComment(commentId);
        CommentLike existing = commentLikeRepository.findByCommentIdAndUserId(commentId, userId).orElse(null);
        if (existing == null) {
            return;
        }
        commentLikeRepository.delete(existing);
        commentRepository.decrementLikes(commentId);
    }

    @Override
    public long getCommentCount(Long postId) {
        if (postId == null) {
            return commentRepository.countByStatusNot(CommentStatus.DELETED);
        }
        return commentRepository.countByPostIdAndStatusNot(postId, CommentStatus.DELETED);
    }

    @Override
    public boolean getModerationMode() {
        initializeModerationModeIfNeeded();
        return moderationMode;
    }

    @Override
    public void updateModerationMode(ModerationModeRequest request) {
        initializeModerationModeIfNeeded();
        moderationMode = request.isModerationRequired();
    }

    private Comment findComment(Long commentId) {
        return commentRepository.findByCommentId(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
    }

    private void initializeModerationModeIfNeeded() {
        if (!moderationModeInitialized) {
            moderationMode = moderationRequired;
            moderationModeInitialized = true;
        }
    }

    private boolean isPostOwner(Long postId, Long actorId) {
        try {
            ResponseEntity<PostAuthorResponse> response = restTemplate.getForEntity(
                    postServiceUrl + "/posts/" + postId,
                    PostAuthorResponse.class);
            PostAuthorResponse body = response.getBody();
            return body != null && actorId.equals(body.getAuthorId());
        } catch (Exception ignored) {
            return false;
        }
    }

    private String resolveAuthorName(String authorName, Long authorId) {
        if (authorName != null && !authorName.isBlank()) {
            return authorName.trim();
        }
        return "User #" + authorId;
    }

    private void dispatchReplyAndMentionNotifications(Comment comment, Comment parentComment, Set<Long> alreadyNotified) {
        Long actorId = comment.getAuthorId();
        String actorName = resolveAuthorName(comment.getAuthorName(), actorId);

        if (parentComment != null && parentComment.getAuthorId() != null && !parentComment.getAuthorId().equals(actorId)) {
            sendNotification(
                    parentComment.getAuthorId(),
                    actorId,
                    "COMMENT_REPLY",
                    actorName + " replied to your comment",
                    comment.getContent(),
                    comment.getCommentId(),
                    "COMMENT");
            alreadyNotified.add(parentComment.getAuthorId());
        }

        Set<String> mentionedUsernames = extractMentions(comment.getContent());
        for (String username : mentionedUsernames) {
            AppUser user = appUserRepository.findByUsernameIgnoreCase(username).orElse(null);
            if (user == null || !user.isActive()) {
                continue;
            }
            if (user.getUserId() == null || user.getUserId().equals(actorId)) {
                continue;
            }
            if (alreadyNotified.contains(user.getUserId())) {
                continue;
            }
            sendNotification(
                    user.getUserId(),
                    actorId,
                    "MENTION",
                    actorName + " mentioned you in a comment",
                    comment.getContent(),
                    comment.getCommentId(),
                    "COMMENT");
            alreadyNotified.add(user.getUserId());
        }
    }

    private void notifyPostAuthorIfNeeded(Comment comment, Long actorId, String actorName, Set<Long> alreadyNotified) {
        PostSummaryResponse post = getPostSummary(comment.getPostId());
        if (post == null || post.getAuthorId() == null || post.getAuthorId().equals(actorId)) {
            return;
        }
        if (alreadyNotified.contains(post.getAuthorId())) {
            return;
        }
        String postTitle = post.getTitle() == null || post.getTitle().isBlank()
                ? "Post #" + comment.getPostId()
                : post.getTitle().trim();
        String commentPreview = summarize(comment.getContent(), 180);
        sendNotification(
                post.getAuthorId(),
                actorId,
                "NEW_COMMENT",
                actorName + " commented on your post",
                "Post: " + postTitle + " | Comment: " + commentPreview,
                comment.getCommentId(),
                "COMMENT");
        alreadyNotified.add(post.getAuthorId());
    }

    private PostSummaryResponse getPostSummary(Long postId) {
        try {
            ResponseEntity<PostSummaryResponse> response = restTemplate.getForEntity(
                    postServiceUrl + "/posts/" + postId,
                    PostSummaryResponse.class);
            return response.getBody();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Set<String> extractMentions(String content) {
        Set<String> usernames = new LinkedHashSet<>();
        if (content == null || content.isBlank()) {
            return usernames;
        }
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            usernames.add(matcher.group(1).toLowerCase(Locale.ROOT));
        }
        return usernames;
    }

    private String summarize(String value, int limit) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= limit) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, limit - 3)) + "...";
    }

    private void sendNotification(
            Long recipientId,
            Long actorId,
            String type,
            String title,
            String message,
            Long relatedId,
            String relatedType) {
        NotificationDispatchEvent inAppEvent = NotificationDispatchEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .sourceService(applicationName)
                .dispatchChannel("IN_APP")
                .recipientId(recipientId)
                .actorId(actorId)
                .type(type)
                .title(title)
                .message(message)
                .relatedId(relatedId)
                .relatedType(relatedType)
                .occurredAt(Instant.now())
                .build();
        publishNotificationEvent(inAppEvent);

        NotificationDispatchEvent emailEvent = NotificationDispatchEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .sourceService(applicationName)
                .dispatchChannel("EMAIL")
                .recipientId(recipientId)
                .actorId(actorId)
                .type(type)
                .title(title)
                .message(message)
                .relatedId(relatedId)
                .relatedType(relatedType)
                .occurredAt(Instant.now())
                .build();
        publishNotificationEvent(emailEvent);
    }

    private void publishNotificationEvent(NotificationDispatchEvent event) {
        if (notificationKafkaTemplateProvider == null) {
            return;
        }
        KafkaTemplate<String, NotificationDispatchEvent> notificationKafkaTemplate =
                notificationKafkaTemplateProvider.getIfAvailable();
        if (notificationKafkaTemplate == null) {
            return;
        }
        String key = event.getRecipientId() == null ? UUID.randomUUID().toString() : String.valueOf(event.getRecipientId());
        notificationKafkaTemplate.send(notificationKafkaTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Kafka publish failed for comment notification event {}", event.getEventId(), ex);
                    }
                });
    }

    private CommentResponse toResponse(Comment comment) {
        return CommentResponse.builder()
                .commentId(comment.getCommentId())
                .postId(comment.getPostId())
                .authorId(comment.getAuthorId())
                .authorName(comment.getAuthorName())
                .parentCommentId(comment.getParentCommentId())
                .content(comment.getContent())
                .likesCount(comment.getLikesCount())
                .status(comment.getStatus())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    private void ensureModerationPermission(Comment comment, Long actorId, String actorRole) {
        if (actorId == null) {
            throw new IllegalArgumentException("actorId is required");
        }
        boolean isAdmin = "ADMIN".equalsIgnoreCase(actorRole == null ? "" : actorRole);
        if (isAdmin) {
            return;
        }
        if (!isPostOwner(comment.getPostId(), actorId)) {
            throw new IllegalArgumentException("You are not allowed to moderate this comment");
        }
    }

    private static class PostAuthorResponse {
        private Long authorId;

        public Long getAuthorId() {
            return authorId;
        }

        public void setAuthorId(Long authorId) {
            this.authorId = authorId;
        }
    }

    private static class PostSummaryResponse {
        private Long authorId;
        private String title;

        public Long getAuthorId() {
            return authorId;
        }

        public void setAuthorId(Long authorId) {
            this.authorId = authorId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

}
