package com.game3alhaq.controller;

import com.game3alhaq.dto.request.StaffEmailRequests.CreateStaffEmailRequest;
import com.game3alhaq.dto.response.StaffEmailResponses.StaffEmailResponse;
import com.game3alhaq.service.StaffEmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffEmailController {

    private final StaffEmailService staffEmailService;

    @GetMapping
    public ResponseEntity<List<StaffEmailResponse>> getAllStaff() {
        return ResponseEntity.ok(staffEmailService.getAllStaffEmails());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StaffEmailResponse> addStaff(@Valid @RequestBody CreateStaffEmailRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(staffEmailService.addStaffEmail(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStaff(@PathVariable Long id) {
        staffEmailService.deleteStaffEmail(id);
        return ResponseEntity.noContent().build();
    }
}
