package com.vstream.video_service.controller;

import com.vstream.video_service.constant.AppConstants;
import com.vstream.video_service.dto.UploadVideoDTO;
import com.vstream.video_service.dto.VideoMetadataDTO;
import com.vstream.video_service.model.VideoMetadata;
import com.vstream.video_service.service.VideoLikeService;
import com.vstream.video_service.service.VideoMetadataService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.jpa.repository.Query;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/videos")
@Slf4j
@CrossOrigin(origins = "http://localhost:3000")
public class VideoMetadataController {

    @Autowired
    private VideoMetadataService videoMetadataService;

    @Autowired
    private VideoLikeService videoLikeService;

    private final String hlsBaseDir = "/home/surajp2909/Videos/vstream"; // Base directory for HLS files

    @GetMapping("/hls/{uploaderId}/{videoId}/{fileName}")
    public ResponseEntity<Resource> streamHLSFile(
            @PathVariable String uploaderId,
            @PathVariable String videoId,
            @PathVariable String fileName) {
        try {
            // Build the file path
            Path filePath = Paths.get(AppConstants.videoStorageDir, uploaderId, videoId, fileName);
            log.info("Attempting to stream HLS file from path: {}", filePath);

            // Load the file as a resource
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                // Set content type based on file type
                log.info("Successfully loaded HLS file: {}", fileName);

                String contentType = fileName.endsWith(".m3u8") ? "application/vnd.apple.mpegurl" : "video/MP2T";
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                        .body(resource);
            } else {
                log.error("File not found or unreadable: {}", filePath);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } catch (Exception e) {
//            e.printStackTrace();
            log.error("Error streaming HLS file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<VideoMetadata> uploadVideo(
            @ModelAttribute UploadVideoDTO uploadVideoDTO
    ) {
        try {
            log.info("Received upload request for video: {}", uploadVideoDTO.getTitle());
            VideoMetadata savedMetadata = videoMetadataService.uploadVideo(uploadVideoDTO);
            log.info("Successfully uploaded video metadata with ID: {}", savedMetadata.getVideoId());

            return ResponseEntity.ok(savedMetadata);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("VideoMetadataController::uploadVideo {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/{videoId}")
    public ResponseEntity<VideoMetadataDTO> getVideoMetadata(@PathVariable String videoId) {
        log.info("Fetching metadata for video with ID: {}", videoId);

        return videoMetadataService
                .getVideoMetadataById(videoId)
                .map(metadata -> {
                    // Log success and map VideoMetadata to VideoMetadataDTO
                    log.info("Successfully retrieved metadata for video ID: {}", videoId);

                    // Mapping VideoMetadata to VideoMetadataDTO
                    VideoMetadataDTO videoMetadataDTO = new VideoMetadataDTO();
                    videoMetadataDTO.setVideoId(metadata.getVideoId().toString());
                    videoMetadataDTO.setTitle(metadata.getTitle());
                    videoMetadataDTO.setUploaderId(metadata.getUploaderId());
                    videoMetadataDTO.setDescription(metadata.getDescription());
                    videoMetadataDTO.setThumbnailUrl(metadata.getThumbnailUrl());
                    videoMetadataDTO.setDuration(metadata.getDuration());
                    videoMetadataDTO.setFileSize(metadata.getFileSize());
                    videoMetadataDTO.setUploadDate(metadata.getUploadDate());
                    videoMetadataDTO.setVideoUrl(metadata.getVideoUrl());

                    return ResponseEntity.ok(videoMetadataDTO);
                })
                .orElseGet(() -> {
                    log.error("No metadata found for video ID: {}", videoId);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping
    public ResponseEntity<List<VideoMetadataDTO>> getAllVideos(
            @RequestParam Optional<String> userId,
            @RequestParam Optional<Boolean> uploadInProgress) {

        List<VideoMetadataDTO> videos = videoMetadataService.getAllVideos(userId, uploadInProgress);

        if (videos.isEmpty()) {
            return ResponseEntity.noContent().build();  // No videos found
        }

        return ResponseEntity.ok(videos);  // Return the filtered list of videos
    }

    // API to update view count
    @PutMapping("/{videoId}/views")
    public ResponseEntity<VideoMetadataDTO> updateViewCount(@PathVariable String videoId) {
        try {
            // Call the service method to update the view count
            VideoMetadataDTO updatedMetadataDTO = videoMetadataService.incrementViewCount(videoId);
            if (updatedMetadataDTO == null) {
                log.error("Video not found with ID: {}", videoId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            return ResponseEntity.ok(updatedMetadataDTO);
        } catch (Exception e) {
            log.error("Error updating view count for video ID: {}", videoId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // API to update like count
    @Transactional
    @PutMapping("/{videoId}/likes")
    public ResponseEntity<?> updateLikeCount(@PathVariable String videoId,
                                                            @RequestParam String userId) {
        try {
            // Check if the user has already liked the video



            if (videoLikeService.checkIfUserVideoMappingExist(userId, videoId)) {
                log.error("User {} has already liked video with ID: {}", userId, videoId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("User can only like the video once.");  // User can only like the video once
            }

            // Proceed with the like operation, increment the like count
            VideoMetadataDTO updatedMetadataDTO = videoMetadataService.incrementLikeCount(videoId);

            if (updatedMetadataDTO == null) {
                log.error("Video not found with ID: {}", videoId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            // Save the like record to prevent duplicate likes
            videoLikeService.likeVideo(UUID.fromString(userId), UUID.fromString(videoId));

            return ResponseEntity.ok(updatedMetadataDTO);

        } catch (Exception e) {
            log.error("Error updating like count for video ID: {}", videoId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }


}

