package com.vstream.video_service.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class UpdateVideoDTO {
    private String title; // Optional
    private String description; // Optional
    private MultipartFile thumbnailFile; // Optional
}
