package com.game3alhaq.controller;

import com.game3alhaq.dto.request.ContentRequests.PrayerTimesUpdateRequest;
import com.game3alhaq.dto.response.ContentResponses.HistoryResponse;
import com.game3alhaq.dto.response.ContentResponses.PrayerTimesResponse;
import com.game3alhaq.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
@Tag(name = "Content", description = "Endpoints for Oman prayer times and mosque history")
public class ContentController {

    private final ContentService contentService;

    @GetMapping("/prayer-times")
    @Operation(summary = "Get daily prayer times for Seeb, Oman")
    public ResponseEntity<PrayerTimesResponse> getPrayerTimes() {
        return ResponseEntity.ok(contentService.getPrayerTimes());
    }

    @PutMapping("/prayer-times")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Manually update or override prayer times (Admin)")
    public ResponseEntity<PrayerTimesResponse> updatePrayerTimes(
            @Valid @RequestBody PrayerTimesUpdateRequest request) {
        return ResponseEntity.ok(contentService.updatePrayerTimes(request));
    }

    @GetMapping("/history")
    @Operation(summary = "Get mosque history, statistics, and facilities overview")
    public ResponseEntity<HistoryResponse> getHistory() {
        return ResponseEntity.ok(contentService.getHistory());
    }
}
