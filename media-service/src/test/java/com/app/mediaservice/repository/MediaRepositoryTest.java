package com.app.mediaservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.app.mediaservice.entity.Media;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:mediadb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class MediaRepositoryTest {

    @Autowired
    private MediaRepository mediaRepository;

    @Test
    void repositorySupportsCommonLookups() {
        Media media = mediaRepository.saveAndFlush(Media.builder()
                .uploaderId(1L)
                .filename("image.png")
                .originalName("image.png")
                .url("http://localhost/image.png")
                .mimeType("image/png")
                .sizeKb(1L)
                .altText("alt")
                .linkedPostId(10L)
                .isDeleted(false)
                .uploadedAt(LocalDateTime.now())
                .build());

        assertThat(mediaRepository.findByMediaId(media.getMediaId())).contains(media);
        assertThat(mediaRepository.findByUploaderId(1L)).contains(media);
        assertThat(mediaRepository.findByLinkedPostId(10L)).contains(media);
        assertThat(mediaRepository.findByMimeType("image/png")).contains(media);
        assertThat(mediaRepository.findByIsDeleted(false)).contains(media);
        assertThat(mediaRepository.countByUploaderId(1L)).isEqualTo(1L);
    }
}

