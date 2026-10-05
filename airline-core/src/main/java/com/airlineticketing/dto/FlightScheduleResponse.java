package com.airlineticketing.dto;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;

public record FlightScheduleResponse(
        Long id,
        String flightNumber,
        String sourceAirportCode,
        String destinationAirportCode,
        LocalTime departureTime,
        LocalTime arrivalTime,
        Long aircraftId,
        String aircraftType,
        Set<DayOfWeek> daysOfOperation,
        Instant createdAt) {
}
