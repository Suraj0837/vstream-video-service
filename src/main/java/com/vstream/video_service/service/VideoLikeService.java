package com.vstream.video_service.service;

import com.vstream.video_service.model.VideoLike;
import com.vstream.video_service.repository.VideoLikeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class VideoLikeService {
    private final VideoLikeRepository videoLikeRepository;

    @Autowired
    public VideoLikeService(VideoLikeRepository videoLikeRepository) {
        this.videoLikeRepository = videoLikeRepository;
    }

    // Method to like a video
    public boolean likeVideo(UUID userId, UUID videoId) {
        // Check if the user has already liked the video
        VideoLike.VideoLikeId videoLikeId = new VideoLike.VideoLikeId(userId, videoId);
        Optional<VideoLike> existingLike = videoLikeRepository.findById(videoLikeId);
        if (existingLike.isPresent()) {
            // User has already liked the video, so return false
            return false;
        }

        // Add a new like
        VideoLike newLike = new VideoLike();
        newLike.setVideoLikeId(videoLikeId);  // Set the composite primary key
        videoLikeRepository.save(newLike);
        return true;
    }

    // Method to unlike a video
    public boolean unlikeVideo(UUID userId, UUID videoId) {
        // Check if the user has liked the video
        VideoLike.VideoLikeId videoLikeId = new VideoLike.VideoLikeId(userId, videoId);
        Optional<VideoLike> existingLike = videoLikeRepository.findById(videoLikeId);
        if (existingLike.isEmpty()) {
            // No like found to remove
            return false;
        }

        // Remove the like
        videoLikeRepository.delete(existingLike.get());
        return true;
    }

    // Method to get the like count for a video
    public long getLikeCount(UUID videoId) {
        return videoLikeRepository.countByVideoLikeId_VideoId(videoId);
    }

    public boolean checkIfUserVideoMappingExist(String userId, String videoId) {
        VideoLike.VideoLikeId videoLikeId = new VideoLike.VideoLikeId(UUID.fromString(userId), UUID.fromString(videoId));
        Optional<VideoLike> existingLike = videoLikeRepository.findById(videoLikeId);
        if (existingLike.isPresent()) return true;
        return false;
    }


}
