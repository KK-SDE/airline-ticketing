package com.airlineticketing.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class FlightTimeUtilsTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 5);

    @Test
    void departureIsDateAtTimeInUtc() {
        assertThat(FlightTimeUtils.departureAt(DATE, LocalTime.of(9, 30)))
                .isEqualTo(Instant.parse("2026-10-05T09:30:00Z"));
    }

    @Test
    void arrivalOnSameDayWhenArrivalTimeIsLater() {
        assertThat(FlightTimeUtils.arrivalAt(DATE, LocalTime.of(9, 30), LocalTime.of(13, 45)))
                .isEqualTo(Instant.parse("2026-10-05T13:45:00Z"));
    }

    @Test
    void arrivalRollsToNextDayWhenArrivalTimeIsNotAfterDeparture() {
        assertThat(FlightTimeUtils.arrivalAt(DATE, LocalTime.of(22, 0), LocalTime.of(2, 30)))
                .isEqualTo(Instant.parse("2026-10-06T02:30:00Z"));
    }
}
