package com.airlineticketing.service;

import com.airlineticketing.dto.BookingResponse;
import com.airlineticketing.dto.CreateBookingRequest;
import java.util.UUID;

public interface BookingService {

    BookingResponse create(CreateBookingRequest request);

    BookingResponse get(UUID bookingReference);

    BookingResponse cancel(UUID bookingReference);
}
