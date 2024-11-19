package com.vstream.video_service.service;

import com.vstream.video_service.constant.AppConstants;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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


}

