package com.app.categoryservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_categories", uniqueConstraints = {
        @UniqueConstraint(name = "uk_post_category", columnNames = { "postId", "categoryId" })
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postCategoryId;

    @Column(nullable = false)
    private Long postId;

    @Column(nullable = false)
    private Long categoryId;
}
