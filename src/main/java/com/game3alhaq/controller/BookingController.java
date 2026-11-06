package com.game3alhaq.controller;

import com.game3alhaq.dto.request.BookingRequests.AdminCreateBookingRequest;
import com.game3alhaq.dto.request.BookingRequests.CreateBookingRequest;
import com.game3alhaq.dto.request.BookingRequests.UpdateBookingStatusRequest;
import com.game3alhaq.dto.response.BookingResponses.BookingInitiationResponse;
import com.game3alhaq.dto.response.BookingResponses.BookingResponse;
import com.game3alhaq.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Endpoints for hall reservations, vouchers, and admin management")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Initiate public hall reservation and PayPal checkout")
    public ResponseEntity<BookingInitiationResponse> createBooking(
            @Valid @RequestBody CreateBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(request));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Lookup booking voucher details by reference code (DAR-XXXXXX)")
    public ResponseEntity<BookingResponse> getBookingByCode(@PathVariable String code) {
        return ResponseEntity.ok(bookingService.getBookingByCode(code));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking details by ID")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    @GetMapping("/my")
    @Operation(summary = "Get bookings of the currently authenticated user")
    public ResponseEntity<List<BookingResponse>> getMyBookings(Authentication authentication) {
        return ResponseEntity.ok(bookingService.getUserBookings(authentication.getName()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all bookings with optional filters (Admin only)")
    public ResponseEntity<List<BookingResponse>> getAllBookingsAdmin(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String bookingType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(bookingService.getAllBookingsAdmin(status, bookingType, fromDate, toDate));
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Direct manual booking creation by Admin")
    public ResponseEntity<BookingResponse> adminCreateBooking(
            @Valid @RequestBody AdminCreateBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.adminCreateBooking(request));
    }

    @RequestMapping(value = "/{id}/status", method = {RequestMethod.PATCH, RequestMethod.PUT})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update booking status (Admin only)")
    public ResponseEntity<BookingResponse> updateBookingStatus(
            @PathVariable Long id,
            @RequestParam(required = false) String status,
            @Valid @RequestBody(required = false) UpdateBookingStatusRequest request) {
        String finalStatus = (status != null && !status.isBlank())
                ? status
                : (request != null ? request.getStatus() : null);
        UpdateBookingStatusRequest req = UpdateBookingStatusRequest.builder().status(finalStatus).build();
        return ResponseEntity.ok(bookingService.updateBookingStatus(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete booking (Admin only)")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }
}
