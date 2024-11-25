package com.vstream.video_service.service;

import com.vstream.video_service.constant.AppConstants;
import com.vstream.video_service.dto.UpdateVideoDTO;
import com.vstream.video_service.dto.UploadVideoDTO;
import com.vstream.video_service.dto.VideoMetadataDTO;
import com.vstream.video_service.model.VideoMetadata;
import com.vstream.video_service.repository.VideoMetadataRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.*;
import java.util.stream.Collectors;

import static com.vstream.video_service.constant.AppConstants.thumbnailStorageDir;
import static com.vstream.video_service.constant.AppConstants.videoStorageDir;

@Slf4j
@Service
public class VideoMetadataService {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private VideoMetadataRepository videoMetadataRepository;

    @Autowired
    private FileService fileService;

    @Autowired
    private VideoUtilityService videoUtilityService;

    @Transactional
    public VideoMetadata uploadVideo(UploadVideoDTO uploadVideoDTO) throws Exception {
        log.info("Starting video upload process for uploader ID: {}", uploadVideoDTO.getUploaderId());

        // Update metadata and save to database
        VideoMetadata videoMetadata = new VideoMetadata();
        videoMetadata.setUploaderId(uploadVideoDTO.getUploaderId());
        videoMetadata.setTitle(uploadVideoDTO.getTitle());
        videoMetadata.setUploadDate(LocalDateTime.now());
        log.debug("Video metadata created: {}", videoMetadata);




        VideoMetadata savedMetadata = videoMetadataRepository.save(videoMetadata);
        log.info("Video metadata saved to the database with ID: {}", savedMetadata.getVideoId());

        String relativeVideoPath = uploadVideoDTO.getUploaderId() + "/" + videoMetadata.getVideoId() + ".mp4";
        Path videoFilePath = Paths.get(videoStorageDir, relativeVideoPath);
        Files.createDirectories(videoFilePath.getParent());
        log.debug("Created directories for video storage if not already present. Path: {}", videoFilePath.getParent());
        uploadVideoDTO.getVideoFile().transferTo(videoFilePath.toFile());

        // Generate a unique filename for the video file

        String relativeThumbnailPath = uploadVideoDTO.getUploaderId() + "/"
                + fileService.getFileName(uploadVideoDTO.getThumbnailFile());
        Path thumbnailFilePath = Paths.get(thumbnailStorageDir, relativeThumbnailPath);
        Files.createDirectories(thumbnailFilePath.getParent());
        log.debug("Created directories for video storage if not already present. Path: {}", thumbnailFilePath.getParent());
        uploadVideoDTO.getThumbnailFile().transferTo(thumbnailFilePath.toFile());
        log.info("Video file and thumbnail saved successfully. Path: {}", videoFilePath);

        // Process HLS chunks
        log.info("Starting HLS chunking for video file at: {}", videoFilePath);
        videoUtilityService.createHLSChunks(videoFilePath, uploadVideoDTO.getUploaderId(), savedMetadata.getVideoId());
        log.info("HLS chunking completed for video file at: {}", videoFilePath);

        savedMetadata.setVideoUrl(relativeVideoPath);
        savedMetadata.setThumbnailUrl(relativeThumbnailPath);
        videoMetadata.setDuration(fileService.getVideoDuration(videoFilePath));
        videoMetadata.setFileSize(fileService.getFileSize(uploadVideoDTO.getVideoFile()));
        videoMetadata.setUploadInProgress(true);

        return videoMetadataRepository.save(savedMetadata);
    }

    public Optional<VideoMetadata> getVideoMetadataById(String videoId) {
        log.info("Retrieving video metadata for ID: {}", videoId);
        Optional<VideoMetadata> videoMetadata = videoMetadataRepository.findByVideoId(UUID.fromString(videoId));
        if (videoMetadata.isPresent()) {
            log.info("Video metadata found for ID: {}", videoId);
        } else {
            log.warn("Video metadata not found for ID: {}", videoId);
        }
        return videoMetadata;
    }



    // Fetch all videos with filtering options
    public List<VideoMetadataDTO> getAllVideos(Optional<String> userId, Optional<Boolean> uploadInProgress) {
        List<VideoMetadata> videos;

        if (userId.isPresent() && uploadInProgress.isPresent()) {
            // If both filters are provided, use the filter method
            videos = videoMetadataRepository.findByUploaderIdAndUploadInProgress(userId.get(), uploadInProgress.get());
        } else if (userId.isPresent()) {
            // If only userId filter is provided
            videos = videoMetadataRepository.findByUploaderIdAndUploadInProgress(userId.get(), false); // Default to false
        } else if (uploadInProgress.isPresent()) {
            // If only uploadInProgress filter is provided
            videos = videoMetadataRepository.findByUploadInProgress(uploadInProgress.get());
        } else {
            // If no filters are provided, return all videos
            videos = videoMetadataRepository.findAll();
        }

        return videos.stream()
                .map(this::convertToDTO)  // Convert VideoMetadata to VideoMetadataDTO
                .collect(Collectors.toList());
    }

    public VideoMetadataDTO incrementViewCount(String videoId) {
        Optional<VideoMetadata> videoMetadataOpt = videoMetadataRepository.findById(UUID.fromString(videoId));
        if (videoMetadataOpt.isPresent()) {
            VideoMetadata videoMetadata = videoMetadataOpt.get();
            videoMetadata.setViewCount(videoMetadata.getViewCount() + 1);
            return modelMapper.map(videoMetadataRepository.save(videoMetadata), VideoMetadataDTO.class);
        } else {
            return null;  // Video not found
        }
    }

    public VideoMetadataDTO convertToDTO(VideoMetadata videoMetadata) {
        VideoMetadataDTO videoMetadataDTO = new VideoMetadataDTO();
        videoMetadataDTO.setVideoId(String.valueOf(videoMetadata.getVideoId()));
        videoMetadataDTO.setTitle(videoMetadata.getTitle());
        videoMetadataDTO.setUploaderId(videoMetadata.getUploaderId());
        videoMetadataDTO.setDescription(videoMetadata.getDescription());
        videoMetadataDTO.setThumbnailUrl(videoMetadata.getThumbnailUrl());
        videoMetadataDTO.setDuration(videoMetadata.getDuration());
        videoMetadataDTO.setFileSize(videoMetadata.getFileSize());
        videoMetadataDTO.setUploadDate(videoMetadata.getUploadDate());
        videoMetadataDTO.setVideoUrl(videoMetadata.getVideoUrl());
        videoMetadataDTO.setLikeCount(videoMetadata.getLikeCount());
        videoMetadataDTO.setViewCount(videoMetadata.getViewCount());

        return videoMetadataDTO;
    }

    public VideoMetadataDTO incrementLikeCount(String videoId) {
        Optional<VideoMetadata> videoMetadataOpt = videoMetadataRepository.findById(UUID.fromString(videoId));
        if (videoMetadataOpt.isPresent()) {
            VideoMetadata videoMetadata = videoMetadataOpt.get();
            videoMetadata.setLikeCount(videoMetadata.getLikeCount() + 1);
            return modelMapper.map(videoMetadataRepository.save(videoMetadata), VideoMetadataDTO.class);
        } else {
            return null;  // Video not found
        }
    }

    @Transactional
    public boolean deleteVideo(String videoId) {
        Optional<VideoMetadata> videoMetadataOpt = videoMetadataRepository.findByVideoId(UUID.fromString(videoId));

        if (videoMetadataOpt.isEmpty()) {
            return false;
        }

        VideoMetadata videoMetadata = videoMetadataOpt.get();

        // Delete the video and thumbnail files from the storage
        try {
            Path hlsFolderPath = Paths.get(AppConstants.videoStorageDir, videoMetadata.getUploaderId(), videoId);
            Path videoFilePath = Paths.get(videoStorageDir, videoMetadata.getVideoUrl());
            Path thumbnailFilePath = Paths.get(thumbnailStorageDir, videoMetadata.getThumbnailUrl());
            deleteDirectory(hlsFolderPath);

            Files.deleteIfExists(videoFilePath);
            Files.deleteIfExists(thumbnailFilePath);

            // Delete metadata from the database
            videoMetadataRepository.delete(videoMetadata);

            return true;
        } catch (Exception e) {
            log.error("Error while deleting video and its metadata: {}", e.getMessage());
            return false;
        }
    }

    private void deleteDirectory(Path directory) throws Exception {
        if (Files.exists(directory)) {
            Files.walk(directory)
                    .sorted((path1, path2) -> path2.compareTo(path1))  // Delete files before directories
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (Exception e) {
                            log.error("Error deleting file/directory: {}", path, e);
                        }
                    });
        }
    }

    @Transactional
    public VideoMetadata updateVideoDetails(String videoId, UpdateVideoDTO updateVideoDTO) throws Exception {
        // Fetch existing video metadata
        VideoMetadata videoMetadata = videoMetadataRepository.findById(UUID.fromString(videoId))
                .orElseThrow(() -> new Exception("Video with ID " + videoId + " not found"));

        log.info("Found video metadata for ID: {}", videoId);

        // Update title if provided
        if (updateVideoDTO.getTitle() != null && !updateVideoDTO.getTitle().isEmpty()) {
            videoMetadata.setTitle(updateVideoDTO.getTitle());
            log.debug("Updated title: {}", updateVideoDTO.getTitle());
        }

        // Update description if provided
        if (updateVideoDTO.getDescription() != null && !updateVideoDTO.getDescription().isEmpty()) {
            videoMetadata.setDescription(updateVideoDTO.getDescription());
            log.debug("Updated description: {}", updateVideoDTO.getDescription());
        }

        // Update thumbnail if provided
        if (updateVideoDTO.getThumbnailFile() != null && !updateVideoDTO.getThumbnailFile().isEmpty()) {
            String relativeThumbnailPath = videoMetadata.getUploaderId() + "/"
                    + fileService.getFileName(updateVideoDTO.getThumbnailFile());
            Path thumbnailFilePath = Paths.get(thumbnailStorageDir, relativeThumbnailPath);
            Files.createDirectories(thumbnailFilePath.getParent());
            updateVideoDTO.getThumbnailFile().transferTo(thumbnailFilePath.toFile());
            videoMetadata.setThumbnailUrl(relativeThumbnailPath);
            log.debug("Updated thumbnail file: {}", relativeThumbnailPath);
        }

        // Save updated metadata
        VideoMetadata updatedMetadata = videoMetadataRepository.save(videoMetadata);
        log.info("Successfully updated video metadata with ID: {}", updatedMetadata.getVideoId());

        return updatedMetadata;
    }

    public Map<String, Long> getUploadTrends(String period) {
        List<VideoMetadata> videos = videoMetadataRepository.findAll();
        return videos.stream()
                .collect(Collectors.groupingBy(video ->
                        getPeriodKey(LocalDate.from(video.getUploadDate()), period), Collectors.counting()));
    }

    private String getPeriodKey(LocalDate uploadDate, String period) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        switch (period) {
            case "day":
                return uploadDate.format(formatter);
            case "week":
                return uploadDate.getYear() + "-W" + uploadDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            case "month":
                return uploadDate.getYear() + "-" + uploadDate.getMonthValue();
            default:
                throw new IllegalArgumentException("Invalid period: " + period);
        }
    }

    public Map<String, Long> getUploadCountByUser() {
        // This method will get the count of videos uploaded by each user
        return videoMetadataRepository.findAll().stream()
                .collect(Collectors.groupingBy(VideoMetadata::getUploaderId, Collectors.counting()));
    }

    public List<Map<String, Object>> getViewsVsLikes() {
        // This method returns a list of maps with view count and like count
        return videoMetadataRepository.findAll().stream()
                .map(video -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("views", video.getViewCount());
                    data.put("likes", video.getLikeCount());
                    return data;
                })
                .collect(Collectors.toList());
    }

    public Map<String, Long> getLikeCountDistribution() {
        // This method calculates the like count distribution in ranges
        Map<String, Long> distribution = new HashMap<>();
        List<VideoMetadata> allVideos = videoMetadataRepository.findAll();

        for (VideoMetadata video : allVideos) {
            int likeCount = Math.toIntExact(video.getLikeCount());
            String range = getLikeCountRange(likeCount);
            distribution.put(range, distribution.getOrDefault(range, 0L) + 1);
        }
        return distribution;
    }

    private String getLikeCountRange(int likeCount) {
        if (likeCount <= 10) return "0-10";
        if (likeCount <= 50) return "11-50";
        if (likeCount <= 100) return "51-100";
        return "100+";
    }
}

