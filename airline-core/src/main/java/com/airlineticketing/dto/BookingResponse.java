package com.airlineticketing.dto;

import com.airlineticketing.constants.BookingStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BookingResponse(
        UUID bookingReference,
        String flightNumber,
        LocalDate flightDate,
        int passengerCount,
        List<BookingSeatResponse> assignedSeats,
        BookingStatus status) {
}
