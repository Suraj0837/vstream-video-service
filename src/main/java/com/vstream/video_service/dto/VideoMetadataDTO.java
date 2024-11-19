package com.vstream.video_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class VideoMetadataDTO {
    private String videoId;
    private String title;
    private String uploaderId;
    private String description;
    private String thumbnailUrl;
    private String duration;
    private Long fileSize;
    private LocalDateTime uploadDate;
    private String videoUrl;
    private Long likeCount = 0L;
    private Long viewCount = 0L;
}
