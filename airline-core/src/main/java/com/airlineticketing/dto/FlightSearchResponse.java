package com.airlineticketing.dto;

import java.time.Instant;
import java.time.LocalDate;

public record FlightSearchResponse(
        Long flightId,
        String flightNumber,
        String sourceAirportCode,
        String destinationAirportCode,
        LocalDate flightDate,
        Instant departureTime,
        Instant arrivalTime,
        int availableSeats) {
}
