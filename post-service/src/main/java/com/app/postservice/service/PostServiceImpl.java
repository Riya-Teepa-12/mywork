package com.app.postservice.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.beans.factory.ObjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.app.postservice.dto.PostCreateRequest;
import com.app.postservice.dto.PostResponse;
import com.app.postservice.dto.PostUpdateRequest;
import com.app.postservice.entity.AuthorFollow;
import com.app.postservice.entity.Post;
import com.app.postservice.entity.PostLike;
import com.app.postservice.entity.PostStatus;
import com.app.postservice.messaging.NotificationDispatchEvent;
import com.app.postservice.repository.AuthorFollowRepository;
import com.app.postservice.repository.PostLikeRepository;
import com.app.postservice.repository.PostRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private static final Logger log = LoggerFactory.getLogger(PostServiceImpl.class);
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
    private static final Pattern TAGS = Pattern.compile("<[^>]*>");
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final AuthorFollowRepository authorFollowRepository;
    private final Map<String, Set<Long>> viewedSessionPosts = new ConcurrentHashMap<>();
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectProvider<KafkaTemplate<String, NotificationDispatchEvent>> notificationKafkaTemplateProvider;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${inkwell.newsletter-service-url:http://localhost:8086}")
    private String newsletterServiceUrl;

    @Value("${inkwell.notification.kafka.topic:notification.dispatch.v1}")
    private String notificationKafkaTopic;

    @Value("${spring.application.name:post-service}")
    private String applicationName;

    @Value("${inkwell.comment-service-url:http://localhost:8083}")
    private String commentServiceUrl;

    @Value("${inkwell.auth-service-url:http://localhost:8081}")
    private String authServiceUrl;

    @Value("${inkwell.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${inkwell.internal-api-key:ghfyfr7t8hgv7yh}")
    private String internalApiKey;

    @Value("${inkwell.free-author-post-limit:5}")
    private int freeAuthorPostLimit;

    @Value("${inkwell.views.redis-prefix:post:views:session}")
    private String viewRedisPrefix;

    @Value("${inkwell.views.session-ttl-hours:24}")
    private long viewSessionTtlHours;

    @Override
    @Transactional
    public PostResponse createPost(PostCreateRequest request, String actorRole) {
        enforceAuthorPostQuota(request.getAuthorId(), actorRole);
        PostStatus status = request.getStatus() == null ? PostStatus.DRAFT : request.getStatus();
        String slug = generateUniqueSlug(request.getTitle());
        Post post = Post.builder()
                .authorId(request.getAuthorId())
                .authorName(resolveAuthorName(request.getAuthorName(), request.getAuthorId()))
                .title(request.getTitle().trim())
                .slug(slug)
                .content(request.getContent())
                .excerpt(resolveExcerpt(request.getExcerpt(), request.getContent()))
                .featuredImageUrl(request.getFeaturedImageUrl())
                .status(status)
                .readTimeMin(computeReadTimeMinutes(request.getContent()))
                .publishedAt(status == PostStatus.PUBLISHED ? LocalDateTime.now() : null)
                .build();
        post = postRepository.save(post);
        if (status == PostStatus.PUBLISHED) {
            notifySubscribersForNewPost(post);
            notifyFollowersForNewPost(post);
        }
        return toResponse(post);
    }

    @Override
    public PostResponse getPostById(Long postId) {
        return toResponse(findPost(postId));
    }

    @Override
    public PostResponse getPostBySlug(String slug) {
        Post post = postRepository.findBySlug(slug).orElseThrow(() -> new IllegalArgumentException("Post not found"));
        return toResponse(post);
    }

    @Override
    public List<PostResponse> getPostsByAuthor(Long authorId) {
        return postRepository.findByAuthorIdOrderByCreatedAtDesc(authorId).stream().map(this::toResponse).toList();
    }

    @Override
    public List<PostResponse> getPublishedPostsByAuthor(Long authorId) {
        return postRepository.findByAuthorIdAndStatusOrderByPublishedAtDesc(authorId, PostStatus.PUBLISHED).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<PostResponse> getAllPosts() {
        return postRepository.findAllByOrderByUpdatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Override
    public List<PostResponse> getPublishedPosts() {
        return postRepository.findPublishedOrderByPublishedAtDesc().stream().map(this::toResponse).toList();
    }

    @Override
    public List<PostResponse> getMostViewedPosts(int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 50));
        return postRepository.findAllByOrderByViewCountDescPublishedAtDesc().stream()
                .limit(normalizedLimit)
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<PostResponse> searchPosts(String keyword) {
        return postRepository.searchByTitle(keyword == null ? "" : keyword).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public PostResponse updatePost(Long postId, PostUpdateRequest request) {
        Post post = findPost(postId);
        boolean firstPublish = post.getPublishedAt() == null;
        if (!post.getTitle().equalsIgnoreCase(request.getTitle().trim())) {
            post.setTitle(request.getTitle().trim());
            post.setSlug(generateUniqueSlug(request.getTitle()));
        }
        post.setContent(request.getContent());
        post.setExcerpt(resolveExcerpt(request.getExcerpt(), request.getContent()));
        post.setFeaturedImageUrl(request.getFeaturedImageUrl());
        post.setReadTimeMin(computeReadTimeMinutes(request.getContent()));
        if (request.getStatus() != null) {
            applyStatus(post, request.getStatus());
        }
        post = postRepository.save(post);
        if (firstPublish && post.getStatus() == PostStatus.PUBLISHED) {
            notifySubscribersForNewPost(post);
            notifyFollowersForNewPost(post);
        }
        return toResponse(post);
    }

    @Override
    @Transactional
    public PostResponse publishPost(Long postId) {
        Post post = findPost(postId);
        boolean firstPublish = post.getPublishedAt() == null;
        applyStatus(post, PostStatus.PUBLISHED);
        post = postRepository.save(post);
        if (firstPublish) {
            notifySubscribersForNewPost(post);
            notifyFollowersForNewPost(post);
        }
        return toResponse(post);
    }

    @Override
    @Transactional
    public PostResponse unpublishPost(Long postId) {
        Post post = findPost(postId);
        applyStatus(post, PostStatus.UNPUBLISHED);
        post = postRepository.save(post);
        return toResponse(post);
    }

    @Override
    @Transactional
    public void deletePost(Long postId) {
        Post post = findPost(postId);
        deleteCommentsForPost(post.getPostId());
        postLikeRepository.deleteByPostId(post.getPostId());
        postRepository.delete(post);
    }

    @Override
    @Transactional
    public void incrementViews(Long postId, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId is required");
        }
        findPost(postId);
        if (!registerViewIfFirstTime(postId, sessionId)) {
            return;
        }
        postRepository.incrementViews(postId);
    }

    @Override
    @Transactional
    public void likePost(Long postId, Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        Post post = findPost(postId);
        if (postLikeRepository.findByPostIdAndUserId(postId, userId).isPresent()) {
            return;
        }
        postLikeRepository.save(PostLike.builder().postId(postId).userId(userId).build());
        postRepository.incrementLikes(postId);
        if (post.getAuthorId() != null && !post.getAuthorId().equals(userId)) {
            NotificationDispatchEvent event = NotificationDispatchEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .sourceService(applicationName)
                    .dispatchChannel("IN_APP")
                    .recipientId(post.getAuthorId())
                    .actorId(userId)
                    .type("LIKE")
                    .title("Your post received a new like")
                    .message("Your post received a new like.")
                    .relatedId(post.getPostId())
                    .relatedType("POST")
                    .occurredAt(Instant.now())
                    .build();
            publishNotificationEvent(event);
        }
    }

    @Override
    @Transactional
    public void unlikePost(Long postId, Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        findPost(postId);
        PostLike postLike = postLikeRepository.findByPostIdAndUserId(postId, userId).orElse(null);
        if (postLike == null) {
            return;
        }
        postLikeRepository.delete(postLike);
        postRepository.decrementLikes(postId);
    }

    @Override
    public long getPostCount(Long authorId) {
        if (authorId == null) {
            return postRepository.count();
        }
        return postRepository.countByAuthorId(authorId);
    }

    @Override
    @Transactional
    public PostResponse featurePost(Long postId, boolean featured) {
        Post post = findPost(postId);
        post.setFeatured(featured);
        post = postRepository.save(post);
        return toResponse(post);
    }

    @Override
    @Transactional
    public boolean followAuthor(Long authorId, Long followerId) {
        if (authorId == null || followerId == null) {
            throw new IllegalArgumentException("authorId and followerId are required");
        }
        if (authorId.equals(followerId)) {
            throw new IllegalArgumentException("You cannot follow yourself");
        }
        if (authorFollowRepository.findByAuthorIdAndFollowerId(authorId, followerId).isPresent()) {
            return false;
        }
        authorFollowRepository.save(AuthorFollow.builder().authorId(authorId).followerId(followerId).build());
        return true;
    }

    @Override
    @Transactional
    public boolean unfollowAuthor(Long authorId, Long followerId) {
        if (authorId == null || followerId == null) {
            throw new IllegalArgumentException("authorId and followerId are required");
        }
        AuthorFollow relation = authorFollowRepository.findByAuthorIdAndFollowerId(authorId, followerId).orElse(null);
        if (relation == null) {
            return false;
        }
        authorFollowRepository.delete(relation);
        return true;
    }

    @Override
    public boolean isFollowingAuthor(Long authorId, Long followerId) {
        if (authorId == null || followerId == null) {
            return false;
        }
        return authorFollowRepository.findByAuthorIdAndFollowerId(authorId, followerId).isPresent();
    }

    @Override
    public long getAuthorFollowerCount(Long authorId) {
        if (authorId == null) {
            return 0;
        }
        return authorFollowRepository.countByAuthorId(authorId);
    }

    @Override
    public List<Long> getFollowedAuthorIds(Long followerId) {
        if (followerId == null) {
            return List.of();
        }
        return authorFollowRepository.findByFollowerId(followerId).stream()
                .map(AuthorFollow::getAuthorId)
                .toList();
    }

    private Post findPost(Long postId) {
        return postRepository.findByPostId(postId).orElseThrow(() -> new IllegalArgumentException("Post not found"));
    }

    private void applyStatus(Post post, PostStatus status) {
        post.setStatus(status);
        if (status == PostStatus.PUBLISHED && post.getPublishedAt() == null) {
            post.setPublishedAt(LocalDateTime.now());
        }
        if (status != PostStatus.PUBLISHED) {
            post.setFeatured(false);
        }
    }

    private String generateUniqueSlug(String title) {
        String base = NON_ALNUM.matcher(title.trim().toLowerCase()).replaceAll("-")
                .replaceAll("(^-+|-+$)", "");
        if (base.isBlank()) {
            base = "post";
        }
        String slug = base;
        int count = 2;
        while (postRepository.existsBySlug(slug)) {
            slug = base + "-" + count++;
        }
        return slug;
    }

    private Integer computeReadTimeMinutes(String content) {
        String text = TAGS.matcher(content).replaceAll(" ").trim();
        if (text.isEmpty()) {
            return 1;
        }
        String[] words = text.split("\\s+");
        return Math.max(1, (int) Math.ceil(words.length / 200.0));
    }

    private String resolveExcerpt(String excerpt, String content) {
        if (excerpt != null && !excerpt.isBlank()) {
            return excerpt.length() <= 400 ? excerpt : excerpt.substring(0, 400);
        }
        String text = TAGS.matcher(content).replaceAll(" ").replaceAll("\\s+", " ").trim();
        if (text.length() <= 200) {
            return text;
        }
        return text.substring(0, 200);
    }

    private String resolveAuthorName(String authorName, Long authorId) {
        if (authorName != null && !authorName.isBlank()) {
            return authorName.trim();
        }
        return "User #" + authorId;
    }

    private void notifySubscribersForNewPost(Post post) {
        SendPostNotificationRequest request = new SendPostNotificationRequest();
        request.setPostId(post.getPostId());
        request.setTitle(post.getTitle());
        request.setSlug(post.getSlug());
        request.setExcerpt(post.getExcerpt());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SendPostNotificationRequest> entity = new HttpEntity<>(request, headers);
        try {
            restTemplate.postForEntity(
                    newsletterServiceUrl + "/newsletter/send-post-notification",
                    entity,
                    Object.class);
        } catch (Exception ex) {
            log.warn("Failed to dispatch newsletter post notification for post {}", post.getPostId(), ex);
        }
    }

    private void notifyFollowersForNewPost(Post post) {
        List<Long> recipients = new ArrayList<>(new LinkedHashSet<>(authorFollowRepository.findByAuthorId(post.getAuthorId())
                .stream()
                .map(AuthorFollow::getFollowerId)
                .filter(followerId -> followerId != null && !followerId.equals(post.getAuthorId()))
                .toList()));
        if (recipients.isEmpty()) {
            return;
        }

        String title = "New post from " + resolveAuthorName(post.getAuthorName(), post.getAuthorId());
        String postLink = trimTrailingSlash(frontendUrl) + "/post/" + (post.getSlug() == null ? "" : post.getSlug());
        String message = post.getTitle() + "\nRead now: " + postLink;

        NotificationDispatchEvent inAppEvent = NotificationDispatchEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .sourceService(applicationName)
                .dispatchChannel("IN_APP")
                .recipientIds(recipients)
                .actorId(post.getAuthorId())
                .type("NEW_POST")
                .title(title)
                .message(message)
                .relatedId(post.getPostId())
                .relatedType("POST")
                .occurredAt(Instant.now())
                .build();
        publishNotificationEvent(inAppEvent);
        for (Long recipientId : recipients) {
            NotificationDispatchEvent emailEvent = NotificationDispatchEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .sourceService(applicationName)
                    .dispatchChannel("EMAIL")
                    .recipientId(recipientId)
                    .actorId(post.getAuthorId())
                    .type("NEW_POST")
                    .title(title)
                    .message(message)
                    .relatedId(post.getPostId())
                    .relatedType("POST")
                    .occurredAt(Instant.now())
                    .build();
            publishNotificationEvent(emailEvent);
        }
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
        String key = UUID.randomUUID().toString();
        if (event.getRecipientId() != null) {
            key = String.valueOf(event.getRecipientId());
        } else if (event.getRecipientIds() != null && !event.getRecipientIds().isEmpty()) {
            key = String.valueOf(event.getRecipientIds().get(0));
        }
        notificationKafkaTemplate.send(notificationKafkaTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Kafka publish failed for post notification event {}", event.getEventId(), ex);
                    }
                });
    }

    private void deleteCommentsForPost(Long postId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", internalApiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            restTemplate.exchange(commentServiceUrl + "/comments/post/" + postId,
                    org.springframework.http.HttpMethod.DELETE,
                    entity,
                    Object.class);
        } catch (Exception ex) {
            log.warn("Failed to delete comments for post {}", postId, ex);
        }
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private boolean registerViewIfFirstTime(Long postId, String sessionId) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return registerViewInMemory(postId, sessionId);
        }
        String key = viewRedisPrefix + ":" + sessionId.trim();
        try {
            Long added = redisTemplate.opsForSet().add(key, String.valueOf(postId));
            redisTemplate.expire(key, Duration.ofHours(Math.max(1L, viewSessionTtlHours)));
            return added != null && added > 0;
        } catch (Exception ex) {
            log.debug("Redis unavailable for post view dedupe. Falling back to memory session cache.", ex);
            return registerViewInMemory(postId, sessionId);
        }
    }

    private boolean registerViewInMemory(Long postId, String sessionId) {
        Set<Long> viewed = viewedSessionPosts.computeIfAbsent(sessionId, id -> ConcurrentHashMap.newKeySet());
        if (viewed.contains(postId)) {
            return false;
        }
        viewed.add(postId);
        return true;
    }

    private PostResponse toResponse(Post post) {
        return PostResponse.builder()
                .postId(post.getPostId())
                .authorId(post.getAuthorId())
                .authorName(post.getAuthorName())
                .title(post.getTitle())
                .slug(post.getSlug())
                .content(post.getContent())
                .excerpt(post.getExcerpt())
                .featuredImageUrl(post.getFeaturedImageUrl())
                .status(post.getStatus())
                .readTimeMin(post.getReadTimeMin())
                .viewCount(post.getViewCount())
                .likesCount(post.getLikesCount())
                .featured(post.getFeatured())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .publishedAt(post.getPublishedAt())
                .build();
    }

    private void enforceAuthorPostQuota(Long authorId, String actorRole) {
        if ("ADMIN".equalsIgnoreCase(actorRole == null ? "" : actorRole)) {
            return;
        }
        EntitlementResponse entitlement = fetchEntitlements(authorId);
        if (entitlement == null) {
            throw new IllegalStateException("Unable to verify subscription entitlement");
        }
        if (entitlement.admin || entitlement.authorPostSubscriptionActive) {
            return;
        }
        long usedPosts = postRepository.countByAuthorId(authorId);
        if (usedPosts >= Math.max(1, freeAuthorPostLimit)) {
            throw new IllegalArgumentException(
                    "Free author limit reached. Buy author post subscription to continue posting.");
        }
    }

    private EntitlementResponse fetchEntitlements(Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", internalApiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<EntitlementResponse> response = restTemplate.exchange(
                    trimTrailingSlash(authServiceUrl) + "/auth/internal/entitlements/" + userId,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    EntitlementResponse.class);
            return response.getBody();
        } catch (Exception ex) {
            log.warn("Failed to fetch entitlement for user {}", userId, ex);
            return null;
        }
    }

    private static class SendPostNotificationRequest {
        private Long postId;
        private String title;
        private String slug;
        private String excerpt;

        public Long getPostId() {
            return postId;
        }

        public void setPostId(Long postId) {
            this.postId = postId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSlug() {
            return slug;
        }

        public void setSlug(String slug) {
            this.slug = slug;
        }

        public String getExcerpt() {
            return excerpt;
        }

        public void setExcerpt(String excerpt) {
            this.excerpt = excerpt;
        }
    }

    private static class EntitlementResponse {
        private boolean admin;
        private boolean authorPostSubscriptionActive;

        public boolean isAdmin() {
            return admin;
        }

        public void setAdmin(boolean admin) {
            this.admin = admin;
        }

        public boolean isAuthorPostSubscriptionActive() {
            return authorPostSubscriptionActive;
        }

        public void setAuthorPostSubscriptionActive(boolean authorPostSubscriptionActive) {
            this.authorPostSubscriptionActive = authorPostSubscriptionActive;
        }
    }
}
