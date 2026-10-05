package com.app.postservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "posts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postId;

    @Column(nullable = false)
    private Long authorId;

    @Column(length = 120)
    private String authorName;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    @Column(length = 400)
    private String excerpt;

    @Column(length = 1024)
    private String featuredImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    @Column(nullable = false)
    private Integer readTimeMin;

    @Column(nullable = false)
    private Long viewCount;

    @Column(nullable = false)
    private Long likesCount;

    @Column(nullable = false)
    private Boolean featured;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime publishedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = PostStatus.DRAFT;
        }
        if (readTimeMin == null) {
            readTimeMin = 1;
        }
        if (viewCount == null) {
            viewCount = 0L;
        }
        if (likesCount == null) {
            likesCount = 0L;
        }
        if (featured == null) {
            featured = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
