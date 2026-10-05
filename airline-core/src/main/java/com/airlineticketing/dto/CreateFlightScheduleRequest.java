package com.airlineticketing.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record CreateFlightScheduleRequest(
        @NotNull @Pattern(regexp = "^[A-Z0-9]{2}[0-9]{1,4}$", message = "must be 2 letters/digits followed by 1-4 digits, e.g. XY101")
        String flightNumber,
        @NotNull @Pattern(regexp = "^[A-Za-z]{3}$", message = "must be a 3-letter IATA code")
        String sourceAirportCode,
        @NotNull @Pattern(regexp = "^[A-Za-z]{3}$", message = "must be a 3-letter IATA code")
        String destinationAirportCode,
        @NotNull LocalTime departureTime,
        @NotNull LocalTime arrivalTime,
        @NotNull Long aircraftId,
        @NotEmpty Set<@NotNull DayOfWeek> daysOfOperation) {
}
