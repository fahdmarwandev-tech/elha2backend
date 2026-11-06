package com.game3alhaq.controller;

import com.game3alhaq.dto.request.HallRequests.CreateBlackoutRequest;
import com.game3alhaq.dto.response.HallResponses.BlackoutResponse;
import com.game3alhaq.service.HallBlackoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/blackouts")
@RequiredArgsConstructor
public class HallBlackoutController {

    private final HallBlackoutService blackoutService;

    @GetMapping
    public ResponseEntity<List<BlackoutResponse>> getBlackouts(@RequestParam(required = false) Long hallId) {
        return ResponseEntity.ok(blackoutService.getBlackoutsByHall(hallId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BlackoutResponse> createBlackout(@Valid @RequestBody CreateBlackoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(blackoutService.createBlackout(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBlackout(@PathVariable Long id) {
        blackoutService.deleteBlackout(id);
        return ResponseEntity.noContent().build();
    }
}
