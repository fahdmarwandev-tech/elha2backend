package com.game3alhaq.controller;

import com.game3alhaq.dto.response.BookingResponses.BookingInitiationResponse;
import com.game3alhaq.dto.response.BookingResponses.BookingResponse;
import com.game3alhaq.dto.response.BookingResponses.PaymentResponse;
import com.game3alhaq.entity.Booking;
import com.game3alhaq.entity.Payment;
import com.game3alhaq.mapper.BookingMapper;
import com.game3alhaq.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Endpoints for PayPal payment capture and status lookup")
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingMapper bookingMapper;

    @PostMapping("/paypal/order/{bookingId}")
    @Operation(summary = "Create or retrieve PayPal order for a booking")
    public ResponseEntity<BookingInitiationResponse> createPayPalOrder(@PathVariable Long bookingId) {
        return ResponseEntity.ok(paymentService.initiateOrGetPayPalOrder(bookingId));
    }

    @PostMapping("/paypal/capture/{orderId}")
    @Operation(summary = "Capture PayPal order and confirm booking (Public webhook / client callback)")
    public ResponseEntity<BookingResponse> capturePayPalOrder(@PathVariable String orderId) {
        Booking booking = paymentService.capturePayPalOrder(orderId);
        return ResponseEntity.ok(bookingMapper.toResponse(booking));
    }

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get payment transaction details for a booking")
    public ResponseEntity<PaymentResponse> getPaymentByBookingId(@PathVariable Long bookingId) {
        Payment payment = paymentService.getPaymentByBookingId(bookingId);
        return ResponseEntity.ok(bookingMapper.toPaymentResponse(payment));
    }
}
