package com.vstream.video_service.repository;

import com.vstream.video_service.model.VideoLike;
import com.vstream.video_service.model.VideoMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoLikeRepository extends JpaRepository<VideoLike, VideoLike.VideoLikeId> {
    // You can define custom queries here if needed.
    long countByVideoLikeId_VideoId(UUID videoId);

    // You can also add a method to check if a user has already liked a video
    Optional<VideoLike> findById(VideoLike.VideoLikeId videoLikeId);
}
