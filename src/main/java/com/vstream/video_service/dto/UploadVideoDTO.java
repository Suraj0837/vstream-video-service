package com.vstream.video_service.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Getter
@Setter
public class UploadVideoDTO {
    private MultipartFile videoFile;
    private String title;
    private String uploaderId;
    private String description;
    private MultipartFile thumbnailFile;
}
