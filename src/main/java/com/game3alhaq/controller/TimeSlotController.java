package com.game3alhaq.controller;

import com.game3alhaq.dto.response.HallResponses.HourlyAvailabilityResponse;
import com.game3alhaq.service.TimeSlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class TimeSlotController {

    private final TimeSlotService timeSlotService;

    @GetMapping("/availability")
    public ResponseEntity<HourlyAvailabilityResponse> getMonthlyAvailability(
            @RequestParam Long hallId,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(timeSlotService.getMonthlyAvailability(hallId, year, month));
    }
}
