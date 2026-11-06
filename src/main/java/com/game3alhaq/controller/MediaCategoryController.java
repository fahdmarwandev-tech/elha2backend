package com.game3alhaq.controller;

import com.game3alhaq.dto.request.MediaRequests.CreateCategoryRequest;
import com.game3alhaq.dto.request.MediaRequests.UpdateCategoryRequest;
import com.game3alhaq.dto.response.MediaResponses.CategoryResponse;
import com.game3alhaq.service.MediaCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/media-categories")
@RequiredArgsConstructor
@Tag(name = "Media Categories", description = "Endpoints for managing media and video categories")
public class MediaCategoryController {

    private final MediaCategoryService mediaCategoryService;

    @GetMapping
    @Operation(summary = "List all active media categories")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(mediaCategoryService.getAllCategories());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get media category by ID")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(mediaCategoryService.getCategoryById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new media category (Admin)")
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mediaCategoryService.createCategory(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update media category (Admin)")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id, @Valid @RequestBody UpdateCategoryRequest request) {
        return ResponseEntity.ok(mediaCategoryService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete media category (Admin)")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        mediaCategoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
