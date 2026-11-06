package com.game3alhaq.controller;

import com.game3alhaq.dto.request.HallRequests.UpdateHallConfigRequest;
import com.game3alhaq.dto.response.HallResponses.HallResponse;
import com.game3alhaq.service.HallService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/halls")
@RequiredArgsConstructor
public class HallController {

    private final HallService hallService;

    @GetMapping
    public ResponseEntity<List<HallResponse>> getActiveHalls() {
        return ResponseEntity.ok(hallService.getActiveHalls());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HallResponse> getHallById(@PathVariable Long id) {
        return ResponseEntity.ok(hallService.getHallById(id));
    }

    @PutMapping({"/{id}", "/{id}/config"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HallResponse> updateHallConfig(
            @PathVariable Long id,
            @Valid @RequestBody UpdateHallConfigRequest request) {
        return ResponseEntity.ok(hallService.updateHallConfig(id, request));
    }
}
