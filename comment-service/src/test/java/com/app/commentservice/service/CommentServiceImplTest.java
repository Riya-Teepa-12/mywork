package com.app.commentservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
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

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    @SuppressWarnings("unused")
    private ObjectProvider<KafkaTemplate<String, NotificationDispatchEvent>> notificationKafkaTemplateProvider;

    @Mock
    private KafkaTemplate<String, NotificationDispatchEvent> notificationKafkaTemplate;

    @InjectMocks
    private CommentServiceImpl commentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(commentService, "notificationKafkaTemplateProvider", notificationKafkaTemplateProvider);
        ReflectionTestUtils.setField(commentService, "moderationRequired", true);
        ReflectionTestUtils.setField(commentService, "postServiceUrl", "http://post");
        ReflectionTestUtils.setField(commentService, "moderationModeInitialized", false);
        ReflectionTestUtils.setField(commentService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(commentService, "notificationKafkaTopic", "notification.dispatch.v1");
        ReflectionTestUtils.setField(commentService, "applicationName", "comment-service");
    }

    @Test
    void addCommentCreatesPendingCommentWhenModerationIsRequired() {
        CommentCreateRequest request = new CommentCreateRequest();
        request.setPostId(1L);
        request.setAuthorId(10L);
        request.setAuthorName("Jane");
        request.setContent(" Great post! ");
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setCommentId(100L);
            comment.setCreatedAt(LocalDateTime.now());
            comment.setUpdatedAt(LocalDateTime.now());
            return comment;
        });

        CommentResponse response = commentService.addComment(request);

        assertThat(response.getCommentId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(CommentStatus.PENDING);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void updateCommentAllowsOwnerWithinWindow() {
        Comment existing = Comment.builder()
                .commentId(1L)
                .postId(1L)
                .authorId(10L)
                .authorName("Jane")
                .content("Old")
                .likesCount(0L)
                .status(CommentStatus.APPROVED)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .updatedAt(LocalDateTime.now().minusMinutes(5))
                .build();
        CommentUpdateRequest request = new CommentUpdateRequest();
        request.setAuthorId(10L);
        request.setContent("Updated text");
        when(commentRepository.findByCommentId(1L)).thenReturn(java.util.Optional.of(existing));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentResponse response = commentService.updateComment(1L, request);

        assertThat(response.getContent()).isEqualTo("Updated text");
    }

    @Test
    void likeCommentAndDeleteCommentWorkForOwner() {
        Comment comment = Comment.builder()
                .commentId(7L)
                .postId(1L)
                .authorId(10L)
                .content("Like me")
                .likesCount(0L)
                .status(CommentStatus.APPROVED)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .updatedAt(LocalDateTime.now().minusMinutes(5))
                .build();
        when(commentRepository.findByCommentId(7L)).thenReturn(java.util.Optional.of(comment));
        when(commentLikeRepository.findByCommentIdAndUserId(7L, 20L)).thenReturn(java.util.Optional.empty());
        when(commentLikeRepository.save(any(CommentLike.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(commentRepository.findByParentCommentId(7L)).thenReturn(List.of());

        commentService.likeComment(7L, 20L);
        commentService.deleteComment(7L, 10L, "AUTHOR");

        verify(commentRepository).incrementLikes(7L);
        verify(commentLikeRepository).deleteByCommentIdIn(any());
        verify(commentRepository).deleteByCommentIdIn(any());
    }

    @Test
    void moderationModeCanBeUpdated() {
        ModerationModeRequest request = new ModerationModeRequest();
        request.setModerationRequired(true);
        commentService.updateModerationMode(request);
        assertThat(commentService.getModerationMode()).isTrue();
    }

    @Test
    void readOperationsUseRepositoryMappings() {
        Comment approved = Comment.builder()
                .commentId(2L)
                .postId(1L)
                .authorId(11L)
                .authorName("Reader")
                .content("Nice")
                .likesCount(2L)
                .status(CommentStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Comment deleted = Comment.builder()
                .commentId(3L)
                .postId(1L)
                .authorId(11L)
                .authorName("Reader")
                .content("Deleted")
                .likesCount(0L)
                .status(CommentStatus.DELETED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(commentRepository.findByPostId(1L)).thenReturn(List.of(approved, deleted));
        when(commentRepository.findAll()).thenReturn(List.of(approved, deleted));
        when(commentRepository.findByStatus(CommentStatus.APPROVED)).thenReturn(List.of(approved));
        when(commentRepository.findByCommentId(2L)).thenReturn(java.util.Optional.of(approved));
        when(commentRepository.findByParentCommentId(2L)).thenReturn(List.of(approved, deleted));

        assertThat(commentService.getCommentsByPost(1L)).hasSize(1);
        assertThat(commentService.getAllComments(null)).hasSize(1);
        assertThat(commentService.getAllComments(CommentStatus.APPROVED)).hasSize(1);
        assertThat(commentService.getCommentById(2L).getCommentId()).isEqualTo(2L);
        assertThat(commentService.getReplies(2L)).hasSize(1);
    }

    @Test
    void approveRejectUnlikeAndCountFlow() {
        Comment comment = Comment.builder()
                .commentId(4L)
                .postId(1L)
                .authorId(20L)
                .authorName("Reader")
                .content("Pending")
                .likesCount(1L)
                .status(CommentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(commentRepository.findByCommentId(4L)).thenReturn(java.util.Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(commentLikeRepository.findByCommentIdAndUserId(4L, 30L))
                .thenReturn(java.util.Optional.of(CommentLike.builder().commentId(4L).userId(30L).build()));
        when(commentRepository.countByStatusNot(CommentStatus.DELETED)).thenReturn(7L);
        when(commentRepository.countByPostIdAndStatusNot(1L, CommentStatus.DELETED)).thenReturn(5L);
        when(commentRepository.findByPostId(2L)).thenReturn(List.of(comment));

        assertThat(commentService.approveComment(4L, 99L, "ADMIN").getStatus()).isEqualTo(CommentStatus.APPROVED);
        assertThat(commentService.rejectComment(4L, 99L, "ADMIN").getStatus()).isEqualTo(CommentStatus.REJECTED);
        commentService.unlikeComment(4L, 30L);
        commentService.deleteCommentsByPost(2L);

        assertThat(commentService.getCommentCount(null)).isEqualTo(7L);
        assertThat(commentService.getCommentCount(1L)).isEqualTo(5L);
        verify(commentRepository).decrementLikes(4L);
        verify(commentRepository).deleteByPostId(2L);
    }

    @Test
    void moderationPermissionAndHelperMethodsBranchesAreCovered() {
        Comment pending = Comment.builder()
                .commentId(8L)
                .postId(9L)
                .authorId(1L)
                .content("Hi @john")
                .status(CommentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(commentRepository.findByCommentId(8L)).thenReturn(java.util.Optional.of(pending));

        Object postAuthorBody = buildPostAuthor(99L);
        when(restTemplate.getForEntity(any(String.class), any(Class.class)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(postAuthorBody));

        assertThatThrownBy(() -> commentService.approveComment(8L, 7L, "AUTHOR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not allowed");

        assertThat((String) ReflectionTestUtils.invokeMethod(commentService, "resolveAuthorName", "  ", 3L))
                .isEqualTo("User #3");
        assertThat((java.util.Set<String>) ReflectionTestUtils.invokeMethod(commentService, "extractMentions", "Hi @John and @jane"))
                .containsExactlyInAnyOrder("john", "jane");
        assertThat((String) ReflectionTestUtils.invokeMethod(commentService, "summarize", "a".repeat(300), 20))
                .endsWith("...");
    }

    @Test
    void addCommentApprovedDispatchesReplyMentionAndPostAuthorPaths() {
        enableKafka();
        ReflectionTestUtils.setField(commentService, "moderationRequired", false);
        ReflectionTestUtils.setField(commentService, "moderationModeInitialized", false);

        Comment parent = Comment.builder()
                .commentId(50L)
                .postId(1L)
                .authorId(20L)
                .authorName("Parent")
                .status(CommentStatus.APPROVED)
                .content("parent")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(commentRepository.findByCommentId(50L)).thenReturn(java.util.Optional.of(parent));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setCommentId(51L);
            comment.setCreatedAt(LocalDateTime.now());
            comment.setUpdatedAt(LocalDateTime.now());
            return comment;
        });

        Object postSummary = buildPostSummary(30L, "Post Title");
        when(restTemplate.getForEntity(any(String.class), any(Class.class)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(postSummary));


        AppUser mentioned = new AppUser();
        mentioned.setUserId(40L);
        mentioned.setUsername("john");
        mentioned.setActive(true);
        when(appUserRepository.findByUsernameIgnoreCase("john")).thenReturn(java.util.Optional.of(mentioned));

        CommentCreateRequest request = new CommentCreateRequest();
        request.setPostId(1L);
        request.setParentCommentId(50L);
        request.setAuthorId(10L);
        request.setAuthorName("Actor");
        request.setContent("Hi @john thanks");

        assertThat(commentService.addComment(request).getStatus()).isEqualTo(CommentStatus.APPROVED);
        verify(notificationKafkaTemplate, org.mockito.Mockito.atLeast(3))
                .send(any(String.class), any(String.class), any(NotificationDispatchEvent.class));
    }

    @Test
    void publishNotificationEventHandlesKeyAndUnavailableProviderBranches() {
        enableKafka();
        NotificationDispatchEvent event = NotificationDispatchEvent.builder().recipientId(77L).build();
        ReflectionTestUtils.invokeMethod(commentService, "publishNotificationEvent", event);
        verify(notificationKafkaTemplate).send(any(String.class), any(String.class), any(NotificationDispatchEvent.class));

        ReflectionTestUtils.setField(commentService, "notificationKafkaTemplateProvider", null);
        ReflectionTestUtils.invokeMethod(commentService, "publishNotificationEvent", event);
        verify(notificationKafkaTemplate, org.mockito.Mockito.times(1))
                .send(any(String.class), any(String.class), any(NotificationDispatchEvent.class));
    }

    private void enableKafka() {
        ReflectionTestUtils.setField(commentService, "notificationKafkaTemplateProvider", notificationKafkaTemplateProvider);
        when(notificationKafkaTemplateProvider.getIfAvailable()).thenReturn(notificationKafkaTemplate);
        org.mockito.Mockito.doReturn(java.util.concurrent.CompletableFuture.completedFuture(null))
                .when(notificationKafkaTemplate)
                .send(org.mockito.ArgumentMatchers.nullable(String.class),
                        org.mockito.ArgumentMatchers.nullable(String.class),
                        org.mockito.ArgumentMatchers.any(NotificationDispatchEvent.class));
    }

    private Object buildPostAuthor(Long authorId) {
        try {
            Class<?> type = Class.forName(CommentServiceImpl.class.getName() + "$PostAuthorResponse");
            java.lang.reflect.Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object object = constructor.newInstance();
            ReflectionTestUtils.setField(object, "authorId", authorId);
            return object;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private Object buildPostSummary(Long authorId, String title) {
        try {
            Class<?> type = Class.forName(CommentServiceImpl.class.getName() + "$PostSummaryResponse");
            java.lang.reflect.Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object object = constructor.newInstance();
            ReflectionTestUtils.setField(object, "authorId", authorId);
            ReflectionTestUtils.setField(object, "title", title);
            return object;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
