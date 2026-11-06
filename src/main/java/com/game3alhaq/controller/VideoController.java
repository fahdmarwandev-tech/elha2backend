package com.game3alhaq.controller;

import com.game3alhaq.dto.request.MediaRequests.CreateVideoRequest;
import com.game3alhaq.dto.response.AuthResponses.MessageResponse;
import com.game3alhaq.dto.response.MediaResponses.VideoResponse;
import com.game3alhaq.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
@Tag(name = "Videos", description = "Endpoints for managing and viewing YouTube & uploaded videos")
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
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add a new YouTube video link (Admin)")
    public ResponseEntity<VideoResponse> createVideo(@Valid @RequestBody CreateVideoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(videoService.createVideo(request));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload a direct video file from computer (Admin)")
    public ResponseEntity<VideoResponse> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "categoryName", required = false) String categoryName,
            @RequestParam(value = "duration", required = false) String duration,
            @RequestParam(value = "viewsCount", required = false) String viewsCount,
            @RequestParam(value = "isFeatured", required = false) Boolean isFeatured) throws IOException {
        VideoResponse response = videoService.uploadLocalVideo(
                file, title, description, category, categoryName, duration, viewsCount, isFeatured);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/featured")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Set video as featured (Admin)")
    public ResponseEntity<VideoResponse> setFeaturedVideo(@PathVariable Long id) {
        return ResponseEntity.ok(videoService.setFeaturedVideo(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a video by ID (Admin)")
    public ResponseEntity<MessageResponse> deleteVideo(@PathVariable Long id) {
        videoService.deleteVideo(id);
        return ResponseEntity.ok(new MessageResponse("تم حذف الفيديو بنجاح", true));
    }
}
