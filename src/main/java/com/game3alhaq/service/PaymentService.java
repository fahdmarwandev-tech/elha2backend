package com.game3alhaq.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game3alhaq.config.PayPalConfig;
import com.game3alhaq.dto.response.BookingResponses.BookingInitiationResponse;
import com.game3alhaq.entity.Booking;
import com.game3alhaq.entity.Payment;
import com.game3alhaq.exception.PaymentProcessingException;
import com.game3alhaq.exception.ResourceNotFoundException;
import com.game3alhaq.mapper.BookingMapper;
import com.game3alhaq.repository.BookingRepository;
import com.game3alhaq.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PayPalConfig payPalConfig;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final MailService mailService;
    private final ObjectMapper objectMapper;
    private final BookingMapper bookingMapper;

    @Transactional
    public BookingInitiationResponse initiateOrGetPayPalOrder(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        Payment payment = booking.getPayment();
        if (payment == null || payment.getPaypalOrderId() == null) {
            payment = createPayPalOrder(booking);
        }
        return new BookingInitiationResponse(
                bookingMapper.toResponse(booking),
                payment.getPaypalOrderId(),
                payment.getApprovalUrl()
        );
    }

    @Transactional
    public Payment createPayPalOrder(Booking booking) {
        if (!payPalConfig.isConfigured()) {
            return createMockPayment(booking);
        }
        return createLivePayment(booking);
    }

    private Payment createMockPayment(Booking booking) {
        String mockId = "MOCK-ORDER-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        String approvalUrl = payPalConfig.getCheckoutUrl(mockId);
        Payment payment = Payment.builder()
                .booking(booking)
                .provider("PAYPAL")
                .paypalOrderId(mockId)
                .amount(booking.getTotalAmount())
                .currency(resolveCurrency(booking.getCurrency()))
                .status("CREATED")
                .approvalUrl(approvalUrl)
                .build();
        booking.setPayment(payment);
        return paymentRepository.save(payment);
    }

    private Payment createLivePayment(Booking booking) {
        try {
            String token = fetchAccessToken();
            String currency = resolveCurrency(booking.getCurrency());
            Map<String, Object> body = buildOrderPayload(booking, currency);
            JsonNode res = callPayPalCreateOrder(token, body);
            String orderId = res.path("id").asText();
            String approvalUrl = extractApprovalUrl(res, orderId);
            Payment payment = Payment.builder().booking(booking).provider("PAYPAL").paypalOrderId(orderId)
                    .amount(booking.getTotalAmount()).currency(currency).status("CREATED").approvalUrl(approvalUrl).build();
            booking.setPayment(payment);
            return paymentRepository.save(payment);
        } catch (Exception ex) {
            throw new PaymentProcessingException("Failed to create PayPal order: " + ex.getMessage(), ex);
        }
    }

    @Transactional
    public Booking capturePayPalOrder(String paypalOrderId) {
        Payment payment = paymentRepository.findByPaypalOrderIdWithBooking(paypalOrderId)
                .or(() -> paymentRepository.findByPaypalOrderId(paypalOrderId))
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "paypalOrderId", paypalOrderId));
        Booking booking = payment.getBooking();
        if ("COMPLETED".equalsIgnoreCase(payment.getStatus())) {
            return booking;
        }
        if (!payPalConfig.isConfigured()) {
            return captureMockPayment(payment, booking);
        }
        return captureLivePayment(payment, booking);
    }

    private Booking captureMockPayment(Payment payment, Booking booking) {
        payment.setStatus("COMPLETED");
        payment.setPaypalCaptureId("MOCK-CAP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase());
        payment.setPaidAt(OffsetDateTime.now());
        payment.setPayerEmail(booking.getEmail());
        booking.setStatus("CONFIRMED");
        paymentRepository.save(payment);
        bookingRepository.save(booking);
        mailService.sendBookingConfirmation(booking);
        return booking;
    }

    private Booking captureLivePayment(Payment payment, Booking booking) {
        try {
            String token = fetchAccessToken();
            JsonNode res = callPayPalCaptureOrder(token, payment.getPaypalOrderId());
            String status = res.path("status").asText();
            if (!"COMPLETED".equalsIgnoreCase(status)) {
                throw new PaymentProcessingException("PayPal capture unsuccessful. Status: " + status);
            }
            applyCaptureDetails(payment, res, booking.getEmail());
            booking.setStatus("CONFIRMED");
            paymentRepository.save(payment);
            bookingRepository.save(booking);
            mailService.sendBookingConfirmation(booking);
            return booking;
        } catch (Exception ex) {
            throw new PaymentProcessingException("Failed to capture PayPal order: " + ex.getMessage(), ex);
        }
    }

    @Transactional(readOnly = true)
    public Payment getPaymentByBookingId(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "bookingId", bookingId));
    }

    private String fetchAccessToken() throws Exception {
        String creds = payPalConfig.getClientId() + ":" + payPalConfig.getClientSecret();
        String basicAuth = Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
        RestClient restClient = RestClient.builder().baseUrl(payPalConfig.getBaseUrl()).build();
        String response = restClient.post().uri("/v1/oauth2/token")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=client_credentials")
                .retrieve().body(String.class);
        JsonNode root = objectMapper.readTree(response);
        return root.path("access_token").asText();
    }

    private JsonNode callPayPalCreateOrder(String token, Map<String, Object> body) throws Exception {
        RestClient restClient = RestClient.builder().baseUrl(payPalConfig.getBaseUrl()).build();
        String json = restClient.post().uri("/v2/checkout/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().body(String.class);
        return objectMapper.readTree(json);
    }

    private JsonNode callPayPalCaptureOrder(String token, String orderId) throws Exception {
        RestClient restClient = RestClient.builder().baseUrl(payPalConfig.getBaseUrl()).build();
        String json = restClient.post().uri("/v2/checkout/orders/{id}/capture", orderId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{}").retrieve().body(String.class);
        return objectMapper.readTree(json);
    }

    private void applyCaptureDetails(Payment payment, JsonNode res, String fallbackEmail) {
        String capId = res.path("purchase_units").path(0).path("payments").path("captures").path(0).path("id").asText();
        String payerEmail = res.path("payer").path("email_address").asText(fallbackEmail);
        payment.setStatus("COMPLETED");
        payment.setPaypalCaptureId(capId);
        payment.setPayerEmail(payerEmail);
        payment.setPaidAt(OffsetDateTime.now());
    }

    private Map<String, Object> buildOrderPayload(Booking booking, String currency) {
        Map<String, Object> amount = Map.of("currency_code", currency, "value", booking.getTotalAmount().toPlainString());
        Map<String, Object> unit = Map.of("reference_id", booking.getCode(), "description", "Hall Booking " + booking.getCode(), "amount", amount);
        return Map.of("intent", "CAPTURE", "purchase_units", List.of(unit));
    }

    private String extractApprovalUrl(JsonNode res, String orderId) {
        JsonNode links = res.path("links");
        if (links.isArray()) {
            for (JsonNode link : links) {
                if ("approve".equalsIgnoreCase(link.path("rel").asText())) {
                    return link.path("href").asText();
                }
            }
        }
        return payPalConfig.getCheckoutUrl(orderId);
    }

    private String resolveCurrency(String bookingCurrency) {
        if (bookingCurrency == null || "OMR".equalsIgnoreCase(bookingCurrency)) {
            return "USD";
        }
        return bookingCurrency;
    }
}
