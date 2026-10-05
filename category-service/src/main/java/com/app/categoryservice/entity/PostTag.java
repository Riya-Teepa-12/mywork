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
@Table(name = "post_tags", uniqueConstraints = {
        @UniqueConstraint(name = "uk_post_tag", columnNames = { "postId", "tagId" })
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postTagId;

    @Column(nullable = false)
    private Long postId;

    @Column(nullable = false)
    private Long tagId;
}
