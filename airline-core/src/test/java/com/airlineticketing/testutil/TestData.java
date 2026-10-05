package com.airlineticketing.testutil;

import com.airlineticketing.entity.Aircraft;
import com.airlineticketing.entity.Airport;
import com.airlineticketing.entity.FlightInstance;
import com.airlineticketing.entity.FlightSchedule;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;

public final class TestData {

    public static final Instant NOW = Instant.parse("2026-10-04T10:00:00Z");

    private TestData() {
    }

    public static Airport airport(String code) {
        return Airport.builder().code(code).name(code + " Airport").city(code + " City").country("Country").build();
    }

    public static Aircraft aircraft() {
        return aircraft(30, "ABCDEF");
    }

    public static Aircraft aircraft(int rows, String letters) {
        Aircraft aircraft = Aircraft.builder().aircraftType("A320").seatRows(rows).seatLetters(letters).build();
        aircraft.setId(1L);
        return aircraft;
    }

    public static FlightSchedule schedule() {
        FlightSchedule schedule = FlightSchedule.builder()
                .flightNumber("XY101")
                .source(airport("DXB"))
                .destination(airport("LHR"))
                .departureTime(LocalTime.of(9, 30))
                .arrivalTime(LocalTime.of(13, 45))
                .aircraft(aircraft())
                .daysOfOperation(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
                .build();
        schedule.setId(10L);
        return schedule;
    }

    public static FlightInstance flight(Instant departureAt) {
        FlightInstance flight = FlightInstance.builder()
                .schedule(schedule())
                .flightDate(LocalDate.ofInstant(departureAt, java.time.ZoneOffset.UTC))
                .departureAt(departureAt)
                .arrivalAt(departureAt.plusSeconds(4 * 3600))
                .build();
        flight.setId(100L);
        return flight;
    }

    public static FlightInstance futureFlight() {
        return flight(NOW.plusSeconds(86_400));
    }
}
