package com.vstream.video_service.repository;

import com.vstream.video_service.model.Comment;
import com.vstream.video_service.model.VideoMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByVideoId(UUID videoId);
}
