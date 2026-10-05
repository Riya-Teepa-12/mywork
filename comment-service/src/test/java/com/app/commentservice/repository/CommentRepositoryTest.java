package com.app.commentservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.app.commentservice.entity.AppUser;
import com.app.commentservice.entity.Comment;
import com.app.commentservice.entity.CommentLike;
import com.app.commentservice.entity.CommentStatus;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:commentdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void commentRepositorySupportsLookupsCountsAndDeletes() {
        Comment root = commentRepository.saveAndFlush(comment(1L, null, 10L, CommentStatus.APPROVED, "Root comment"));
        Comment reply = commentRepository.saveAndFlush(comment(1L, root.getCommentId(), 11L, CommentStatus.APPROVED, "Reply"));
        Comment deleted = commentRepository.saveAndFlush(comment(1L, null, 12L, CommentStatus.DELETED, "Deleted"));

        assertThat(commentRepository.findByCommentId(root.getCommentId())).contains(root);
        assertThat(commentRepository.findByPostId(1L)).contains(root, reply, deleted);
        assertThat(commentRepository.findByAuthorId(11L)).containsExactly(reply);
        assertThat(commentRepository.findByParentCommentId(root.getCommentId())).containsExactly(reply);
        assertThat(commentRepository.findTopLevelByPostId(1L)).contains(root, deleted);
        assertThat(commentRepository.countByPostId(1L)).isEqualTo(3L);
        assertThat(commentRepository.findByStatus(CommentStatus.APPROVED)).contains(root, reply);
        assertThat(commentRepository.countByPostIdAndStatusNot(1L, CommentStatus.DELETED)).isEqualTo(2L);
        assertThat(commentRepository.countByStatusNot(CommentStatus.DELETED)).isEqualTo(2L);
    }

    @Test
    void likeAndUserRepositoriesSupportBasicQueries() {
        AppUser user = new AppUser();
        user.setUserId(99L);
        user.setUsername("jane");
        user.setEmail("jane@example.com");
        user.setFullName("Jane Doe");
        user.setActive(true);
        appUserRepository.saveAndFlush(user);

        CommentLike like = commentLikeRepository.saveAndFlush(CommentLike.builder().commentId(5L).userId(99L).build());
        assertThat(commentLikeRepository.findByCommentIdAndUserId(5L, 99L)).contains(like);
        commentLikeRepository.deleteByCommentIdIn(List.of(5L));
        assertThat(commentLikeRepository.findByCommentIdAndUserId(5L, 99L)).isEmpty();

        assertThat(appUserRepository.findByUserId(99L)).contains(user);
        assertThat(appUserRepository.findByUsernameIgnoreCase("JANE")).contains(user);
    }

    private Comment comment(Long postId, Long parentCommentId, Long authorId, CommentStatus status, String content) {
        return Comment.builder()
                .postId(postId)
                .authorId(authorId)
                .authorName("Author")
                .parentCommentId(parentCommentId)
                .content(content)
                .likesCount(0L)
                .status(status)
                .createdAt(LocalDateTime.now().minusHours(2))
                .updatedAt(LocalDateTime.now().minusMinutes(30))
                .build();
    }
}

