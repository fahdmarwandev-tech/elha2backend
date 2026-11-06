package com.game3alhaq.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.game3alhaq.config.PayPalConfig;
import com.game3alhaq.dto.response.BookingResponses.BookingInitiationResponse;
import com.game3alhaq.dto.response.BookingResponses.BookingResponse;
import com.game3alhaq.entity.Booking;
import com.game3alhaq.entity.Hall;
import com.game3alhaq.entity.Payment;
import com.game3alhaq.exception.ResourceNotFoundException;
import com.game3alhaq.mapper.BookingMapper;
import com.game3alhaq.repository.BookingRepository;
import com.game3alhaq.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PayPalConfig payPalConfig;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private MailService mailService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private PaymentService paymentService;

    private Booking booking;

    @BeforeEach
    void setUp() {
        Hall hall = Hall.builder().id(1L).name("قاعة المجلس العام").build();
        booking = Booking.builder()
                .id(10L)
                .code("DAR-123456")
                .hall(hall)
                .fullName("خالد الشبيبي")
                .email("khaled@example.com")
                .totalAmount(BigDecimal.valueOf(50.0))
                .currency("OMR")
                .eventDate(LocalDate.now().plusDays(3))
                .startHour(16)
                .endHour(21)
                .status("PENDING_PAYMENT")
                .build();
    }

    @Test
    @DisplayName("Mock Mode: Create PayPal order creates simulated payment and approval URL")
    void createPayPalOrder_MockMode_Success() {
        when(payPalConfig.isConfigured()).thenReturn(false);
        when(payPalConfig.getCheckoutUrl(anyString())).thenAnswer(inv -> "https://www.sandbox.paypal.com/checkoutnow?token=" + inv.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment payment = paymentService.createPayPalOrder(booking);

        assertNotNull(payment);
        assertEquals("PAYPAL", payment.getProvider());
        assertEquals("CREATED", payment.getStatus());
        assertTrue(payment.getPaypalOrderId().startsWith("MOCK-ORDER-"));
        assertTrue(payment.getApprovalUrl().contains("checkoutnow?token="));
        verify(paymentRepository, times(1)).save(payment);
    }

    @Test
    @DisplayName("Mock Mode: Capture PayPal order updates status to COMPLETED and confirms booking")
    void capturePayPalOrder_MockMode_Success() {
        Payment payment = Payment.builder()
                .id(1L)
                .paypalOrderId("MOCK-ORDER-ABC")
                .booking(booking)
                .status("CREATED")
                .amount(BigDecimal.valueOf(50.0))
                .currency("USD")
                .build();

        when(paymentRepository.findByPaypalOrderIdWithBooking("MOCK-ORDER-ABC")).thenReturn(Optional.of(payment));
        when(payPalConfig.isConfigured()).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        Booking capturedBooking = paymentService.capturePayPalOrder("MOCK-ORDER-ABC");

        assertEquals("CONFIRMED", capturedBooking.getStatus());
        assertEquals("COMPLETED", payment.getStatus());
        assertNotNull(payment.getPaypalCaptureId());
        assertNotNull(payment.getPaidAt());
        verify(mailService, times(1)).sendBookingConfirmation(booking);
        verify(paymentRepository, times(1)).save(payment);
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    @DisplayName("Capture PayPal order returns immediately if payment is already COMPLETED (idempotent)")
    void capturePayPalOrder_AlreadyCompleted_Idempotent() {
        Payment payment = Payment.builder()
                .id(1L)
                .paypalOrderId("MOCK-ORDER-COMPLETED")
                .booking(booking)
                .status("COMPLETED")
                .build();

        when(paymentRepository.findByPaypalOrderIdWithBooking("MOCK-ORDER-COMPLETED")).thenReturn(Optional.of(payment));

        Booking result = paymentService.capturePayPalOrder("MOCK-ORDER-COMPLETED");

        assertEquals(booking, result);
        verify(paymentRepository, never()).save(any());
        verify(mailService, never()).sendBookingConfirmation(any());
    }

    @Test
    @DisplayName("Capture PayPal order throws ResourceNotFoundException if order ID not found")
    void capturePayPalOrder_NotFound_ThrowsException() {
        when(paymentRepository.findByPaypalOrderIdWithBooking("UNKNOWN-ORDER")).thenReturn(Optional.empty());
        when(paymentRepository.findByPaypalOrderId("UNKNOWN-ORDER")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.capturePayPalOrder("UNKNOWN-ORDER"));
    }

    @Test
    @DisplayName("Get payment by booking ID returns payment record")
    void getPaymentByBookingId_Success() {
        Payment payment = Payment.builder().id(99L).build();
        when(paymentRepository.findByBookingId(10L)).thenReturn(Optional.of(payment));

        Payment result = paymentService.getPaymentByBookingId(10L);

        assertEquals(99L, result.getId());
    }

    @Test
    @DisplayName("Initiate or get PayPal order returns initiation response with existing payment")
    void initiateOrGetPayPalOrder_WithExistingPayment_Success() {
        Payment payment = Payment.builder()
                .paypalOrderId("MOCK-EXISTING")
                .approvalUrl("https://paypal.com/approve")
                .build();
        booking.setPayment(payment);

        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(bookingMapper.toResponse(booking)).thenReturn(BookingResponse.builder().id(10L).code("DAR-123456").build());

        BookingInitiationResponse response = paymentService.initiateOrGetPayPalOrder(10L);

        assertNotNull(response);
        assertEquals("MOCK-EXISTING", response.getPaypalOrderId());
        assertEquals("https://paypal.com/approve", response.getApprovalUrl());
        assertEquals("DAR-123456", response.getBooking().getCode());
    }

    @Test
    @DisplayName("Initiate or get PayPal order throws ResourceNotFoundException if booking not found")
    void initiateOrGetPayPalOrder_BookingNotFound_Throws() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.initiateOrGetPayPalOrder(999L));
    }
}
