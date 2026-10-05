package com.app.postservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.app.postservice.entity.AuthorFollow;
import com.app.postservice.entity.Post;
import com.app.postservice.entity.PostLike;
import com.app.postservice.entity.PostStatus;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:postdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@SuppressWarnings({"SpringJavaAutowiredFieldsWarningInspection", "SpringJavaInjectionPointsAutowiringInspection"})
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private AuthorFollowRepository authorFollowRepository;

    @Test
    void postRepositorySupportsLookupAndSearchQueries() {
        Post draft = postRepository.saveAndFlush(post("spring testing", "spring testing content", PostStatus.DRAFT, 1L, 11L));
        Post published = postRepository.saveAndFlush(post("spring data", "repository search body", PostStatus.PUBLISHED, 2L, 22L));

        assertThat(postRepository.findBySlug("spring-testing")).contains(draft);
        assertThat(postRepository.findByAuthorId(1L)).containsExactly(draft);
        assertThat(postRepository.findByAuthorIdOrderByCreatedAtDesc(2L)).containsExactly(published);
        assertThat(postRepository.findByAuthorIdAndStatusOrderByPublishedAtDesc(2L, PostStatus.PUBLISHED)).containsExactly(published);
        assertThat(postRepository.findPublishedOrderByPublishedAtDesc()).containsExactly(published);
        assertThat(postRepository.searchByTitle("search")).containsExactly(published);
        assertThat(postRepository.countByAuthorId(1L)).isEqualTo(1);
        assertThat(postRepository.existsBySlug("spring-testing")).isTrue();
    }

    @Test
    void likeAndFollowRepositoriesSupportLookupAndCounts() {
        PostLike like = postLikeRepository.saveAndFlush(PostLike.builder().postId(9L).userId(7L).build());
        AuthorFollow follow = authorFollowRepository.saveAndFlush(AuthorFollow.builder().authorId(8L).followerId(7L).build());

        assertThat(postLikeRepository.findByPostIdAndUserId(9L, 7L)).contains(like);
        assertThat(postLikeRepository.countByPostId(9L)).isEqualTo(1L);
        postLikeRepository.deleteByPostId(9L);
        assertThat(postLikeRepository.findByPostIdAndUserId(9L, 7L)).isEmpty();

        assertThat(authorFollowRepository.findByAuthorIdAndFollowerId(8L, 7L)).contains(follow);
        assertThat(authorFollowRepository.findByAuthorId(8L)).containsExactly(follow);
        assertThat(authorFollowRepository.findByFollowerId(7L)).containsExactly(follow);
        assertThat(authorFollowRepository.countByAuthorId(8L)).isEqualTo(1L);
    }

    private Post post(String title, String content, PostStatus status, Long authorId, Long likesCount) {
        return Post.builder()
                .authorId(authorId)
                .authorName("Author")
                .title(title)
                .slug(title.toLowerCase().replace(' ', '-'))
                .content(content)
                .excerpt("excerpt")
                .featuredImageUrl("https://example.com/image.png")
                .status(status)
                .readTimeMin(1)
                .viewCount(0L)
                .likesCount(likesCount)
                .featured(false)
                .createdAt(LocalDateTime.now().minusHours(1))
                .updatedAt(LocalDateTime.now().minusMinutes(30))
                .publishedAt(status == PostStatus.PUBLISHED ? LocalDateTime.now().minusMinutes(15) : null)
                .build();
    }
}

