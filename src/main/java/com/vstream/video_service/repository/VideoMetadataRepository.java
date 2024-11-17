package com.vstream.video_service.repository;

import com.vstream.video_service.model.VideoMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VideoMetadataRepository extends JpaRepository<VideoMetadata, UUID> {
    Optional<VideoMetadata> findByVideoId(UUID videoId);

    // Custom query to filter videos by userId and uploadInProgress
    List<VideoMetadata> findByUploaderIdAndUploadInProgress(String uploaderId, Boolean uploadInProgress);

    // Custom query to fetch all videos if no filter is provided
    List<VideoMetadata> findByUploadInProgress(Boolean uploadInProgress);

    // Method to fetch all videos
    List<VideoMetadata> findAll();
}
