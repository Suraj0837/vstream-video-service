package com.vstream.video_service.service;

import com.vstream.video_service.model.VideoMetadata;
import com.vstream.video_service.repository.VideoMetadataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

import static com.vstream.video_service.constant.AppConstants.videoStorageDir;

@Service
@Slf4j
public class VideoUtilityService {

    @Autowired
    private VideoMetadataRepository videoMetadataRepository;

    @Async
    public void createHLSChunks(Path videoFilePath, String uploaderId, UUID videoId) throws Exception {
        log.info("Initiating FFmpeg HLS chunking command for file: {}", videoFilePath);

        // Define output paths for HLS playlist and segments in the desired directory
        Path hlsDirectory = Paths.get(videoStorageDir, uploaderId, videoId.toString());
        Files.createDirectories(hlsDirectory);
        Path playlistPath = hlsDirectory.resolve("index.m3u8");

        String[] command = {
                "ffmpeg", "-i", videoFilePath.toString(),
                "-c:v", "libx264",           // Ensure video is encoded with H.264
                "-c:a", "aac",               // Ensure audio is encoded with AAC
                "-start_number", "0",
                "-hls_time", "10",           // Duration of each segment in seconds
                "-hls_list_size", "0",       // Include all segments in the playlist
                "-hls_segment_filename", hlsDirectory.resolve("segment%d.ts").toString(),
                "-f", "hls",                 // Set output format to HLS
                "-preset", "fast",           // Set encoding speed/quality (optional)
                "-hls_playlist_type", "vod", // For Video on Demand
                playlistPath.toString()      // Output playlist path
        };

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.inheritIO();

        log.debug("Executing FFmpeg command: {}", (Object) command);
        Process process = processBuilder.start();
        int exitCode = process.waitFor();

        if (exitCode == 0) {
            log.info("FFmpeg process completed successfully for file: {}", videoFilePath);
            updateVideoMetadataUploadInProgressFlag(videoId);
        } else {
            log.error("FFmpeg process failed with exit code {} for file: {}", exitCode, videoFilePath);
            throw new Exception("Failed to create HLS chunks for video file: " + videoFilePath);
        }
    }

    private void updateVideoMetadataUploadInProgressFlag(UUID videoId) {
        Optional<VideoMetadata> videoMetadataOptional = videoMetadataRepository.findByVideoId(videoId);
        if (videoMetadataOptional.isPresent()) {
            VideoMetadata videoMetadata = videoMetadataOptional.get();
            videoMetadata.setUploadInProgress(false);
            videoMetadataRepository.save(videoMetadata);
        }
    }

}
