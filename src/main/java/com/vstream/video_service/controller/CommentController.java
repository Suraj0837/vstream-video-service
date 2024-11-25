package com.vstream.video_service.controller;

import com.vstream.video_service.dto.CommentDTO;
import com.vstream.video_service.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:3000")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentDTO> addComment(@RequestBody CommentDTO commentDTO) {
        log.info("Received request to add comment: {}", commentDTO);
        try {
            CommentDTO savedComment = commentService.addComment(commentDTO);
            log.info("Comment added successfully with ID: {}", savedComment.getCommentId());
            return ResponseEntity.ok(savedComment);
        } catch (Exception e) {
            log.error("Error adding comment: {}", commentDTO, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/video/{videoId}")
    public ResponseEntity<List<CommentDTO>> getCommentsByVideo(@PathVariable UUID videoId) {
        log.info("Fetching comments for video ID: {}", videoId);
        try {
            List<CommentDTO> comments = commentService.getCommentsByVideoId(videoId);
            log.info("Fetched {} comments for video ID: {}", comments.size(), videoId);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            log.error("Error fetching comments for video ID: {}", videoId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<CommentDTO> updateComment(
            @PathVariable UUID commentId,
            @RequestBody String content) {
        log.info("Received request to update comment ID: {}", commentId);
        try {
            CommentDTO updatedComment = commentService.updateComment(commentId, content);
            if (updatedComment == null) {
                log.warn("Comment not found with ID: {}", commentId);
                return ResponseEntity.notFound().build();
            }
            log.info("Comment updated successfully with ID: {}", commentId);
            return ResponseEntity.ok(updatedComment);
        } catch (Exception e) {
            log.error("Error updating comment ID: {}", commentId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable UUID commentId) {
        log.info("Received request to delete comment ID: {}", commentId);
        try {
            commentService.deleteComment(commentId);
            log.info("Comment deleted successfully with ID: {}", commentId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting comment ID: {}", commentId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
