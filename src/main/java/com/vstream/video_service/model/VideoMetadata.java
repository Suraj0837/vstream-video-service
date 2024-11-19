package com.vstream.video_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "video_metadata")
@Getter
@Setter
@NoArgsConstructor
public class VideoMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "video_id", nullable = false, unique = true, updatable = false)
    private UUID videoId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "uploader_id", nullable = false)
    private String uploaderId;

    @Column(name = "description")
    private String description;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "duration")
    private String duration;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "upload_date")
    private LocalDateTime uploadDate;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "upload_in_progress", nullable = false)
    private Boolean uploadInProgress = false;

    @Column(name = "like_count", nullable = false)
    private Long likeCount = 0L;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;
}
