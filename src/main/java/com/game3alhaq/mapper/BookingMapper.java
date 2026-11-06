package com.game3alhaq.mapper;

import com.game3alhaq.dto.response.BookingResponses.BookingResponse;
import com.game3alhaq.dto.response.BookingResponses.PaymentResponse;
import com.game3alhaq.entity.Booking;
import com.game3alhaq.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(source = "hall.id", target = "hallId")
    @Mapping(source = "hall.name", target = "hallName")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "payment.id", target = "paymentId")
    @Mapping(source = "payment.paypalOrderId", target = "paypalOrderId")
    @Mapping(source = "payment.status", target = "paymentStatus")
    BookingResponse toResponse(Booking booking);

    List<BookingResponse> toResponseList(List<Booking> bookings);

    @Mapping(source = "booking.id", target = "bookingId")
    PaymentResponse toPaymentResponse(Payment payment);
}
