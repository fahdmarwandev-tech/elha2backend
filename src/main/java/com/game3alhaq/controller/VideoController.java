package com.game3alhaq.controller;

import com.game3alhaq.dto.request.MediaRequests.CreateVideoRequest;
import com.game3alhaq.dto.response.MediaResponses.VideoResponse;
import com.game3alhaq.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
@Tag(name = "Videos", description = "Endpoints for managing and viewing YouTube & uploaded videos")
@CrossOrigin(origins = "*")
public class VideoController {

    private final VideoService videoService;

    @GetMapping
    @Operation(summary = "Get all active videos for public viewing")
    public ResponseEntity<List<VideoResponse>> getAllVideos() {
        return ResponseEntity.ok(videoService.getAllVideos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get specific video details by ID")
    public ResponseEntity<VideoResponse> getVideoById(@PathVariable Long id) {
        return ResponseEntity.ok(videoService.getVideoById(id));
    }

    @PostMapping
    @Operation(summary = "Add a new YouTube video link (Admin)")
    public ResponseEntity<?> createVideo(@Valid @RequestBody CreateVideoRequest request) {
        try {
            VideoResponse created = videoService.createVideo(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a direct video file from computer (Admin)")
    public ResponseEntity<?> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "categoryName", required = false) String categoryName,
            @RequestParam(value = "duration", required = false) String duration,
            @RequestParam(value = "viewsCount", required = false) String viewsCount,
            @RequestParam(value = "isFeatured", required = false) Boolean isFeatured
    ) {
        try {
            VideoResponse created = videoService.uploadLocalVideo(
                    file, title, description, category, categoryName, duration, viewsCount, isFeatured
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "فشل رفع الملف: " + ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a video by ID (Admin)")
    public ResponseEntity<?> deleteVideo(@PathVariable Long id) {
        try {
            videoService.deleteVideo(id);
            return ResponseEntity.ok(Map.of("message", "تم حذف الفيديو بنجاح", "success", true));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
