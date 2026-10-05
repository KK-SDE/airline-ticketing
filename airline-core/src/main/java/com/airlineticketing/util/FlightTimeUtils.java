package com.airlineticketing.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

public final class FlightTimeUtils {

    private FlightTimeUtils() {
    }

    public static Instant departureAt(LocalDate flightDate, LocalTime departureTime) {
        return LocalDateTime.of(flightDate, departureTime).toInstant(ZoneOffset.UTC);
    }

    public static Instant arrivalAt(LocalDate flightDate, LocalTime departureTime, LocalTime arrivalTime) {
        LocalDate arrivalDate = arrivalTime.isAfter(departureTime) ? flightDate : flightDate.plusDays(1);
        return LocalDateTime.of(arrivalDate, arrivalTime).toInstant(ZoneOffset.UTC);
    }
}
