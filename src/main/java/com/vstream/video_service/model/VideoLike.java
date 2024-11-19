package com.vstream.video_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "video_likes")
@Getter
@Setter
@NoArgsConstructor
public class VideoLike {

    @EmbeddedId
    private VideoLikeId videoLikeId; // Composite primary key class

    @Getter
    @Setter
    @Embeddable
    public static class VideoLikeId implements Serializable {

        @Column(name = "user_id")
        private UUID userId;

        @Column(name = "video_id")
        private UUID videoId;

        // Default constructor, hashCode, equals method
        public VideoLikeId() {}

        public VideoLikeId(UUID userId, UUID videoId) {
            this.userId = userId;
            this.videoId = videoId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VideoLikeId that = (VideoLikeId) o;
            return Objects.equals(userId, that.userId) && Objects.equals(videoId, that.videoId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, videoId);
        }
    }
}
