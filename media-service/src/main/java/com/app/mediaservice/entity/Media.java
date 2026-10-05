package com.app.mediaservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "media")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mediaId;

    @Column(nullable = false)
    private Long uploaderId;

    @Column(nullable = false, length = 220)
    private String filename;

    @Column(nullable = false, length = 220)
    private String originalName;

    @Column(nullable = false, length = 1024)
    private String url;

    @Column(nullable = false, length = 120)
    private String mimeType;

    @Column(nullable = false)
    private Long sizeKb;

    @Column(length = 300)
    private String altText;

    private Long linkedPostId;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Column(nullable = false)
    private Boolean isDeleted;

    @PrePersist
    public void prePersist() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
    }
}
