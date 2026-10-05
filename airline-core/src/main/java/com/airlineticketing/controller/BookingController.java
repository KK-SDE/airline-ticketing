package com.airlineticketing.controller;

import com.airlineticketing.dto.BookingResponse;
import com.airlineticketing.dto.CreateBookingRequest;
import com.airlineticketing.service.BookingService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@Valid @RequestBody CreateBookingRequest request) {
        return bookingService.create(request);
    }

    @GetMapping("/{bookingReference}")
    public BookingResponse get(@PathVariable UUID bookingReference) {
        return bookingService.get(bookingReference);
    }

    @PostMapping("/{bookingReference}/cancel")
    public BookingResponse cancel(@PathVariable UUID bookingReference) {
        return bookingService.cancel(bookingReference);
    }
}
