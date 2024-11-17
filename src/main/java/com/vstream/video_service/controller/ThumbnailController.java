package com.vstream.video_service.controller;

import com.vstream.video_service.dto.VideoMetadataDTO;
import com.vstream.video_service.model.VideoMetadata;
import com.vstream.video_service.service.VideoMetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static com.vstream.video_service.constant.AppConstants.thumbnailStorageDir;

@Controller
public class ThumbnailController {

    @Autowired
    private VideoMetadataService videoMetadataService;


    @GetMapping("/thumbnails/{videoId}")
    public ResponseEntity<Resource> serveThumbnail(@PathVariable UUID videoId) throws IOException {
        Optional<VideoMetadata> videoMetadata = videoMetadataService.getVideoMetadataById(videoId.toString());
        // Build the path to the thumbnail file
        Path filePath = Paths.get(thumbnailStorageDir).resolve(videoMetadata.get().getThumbnailUrl()).normalize();

        // Create a resource from the file path
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() || resource.isReadable()) {
            // Return the image file as a response with correct content type
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .contentType(MediaType.IMAGE_PNG) // Adjust the content type based on your image type (JPEG, PNG, etc.)
                    .body(resource);
        } else {
            // Return 404 if the file does not exist or cannot be read
            return ResponseEntity.notFound().build();
        }
    }
}

