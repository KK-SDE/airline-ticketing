package com.airlineticketing.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SeatLabelUtilsTest {

    @Test
    void allLabelsFollowsRowThenLetterOrder() {
        List<String> labels = SeatLabelUtils.allLabels(30, "ABCDEF");

        assertThat(labels).hasSize(180).startsWith("1A", "1B", "1C").endsWith("30F");
    }

    @Test
    void isValidChecksRowAndLetterAgainstConfiguration() {
        assertThat(SeatLabelUtils.isValid("30F", 30, "ABCDEF")).isTrue();
        assertThat(SeatLabelUtils.isValid("31A", 30, "ABCDEF")).isFalse();
        assertThat(SeatLabelUtils.isValid("0A", 30, "ABCDEF")).isFalse();
        assertThat(SeatLabelUtils.isValid("1G", 30, "ABCDEF")).isFalse();
        assertThat(SeatLabelUtils.isValid("1a", 30, "ABCDEF")).isFalse();
        assertThat(SeatLabelUtils.isValid("A1", 30, "ABCDEF")).isFalse();
    }

    @Test
    void normalizeTrimsAndUppercases() {
        assertThat(SeatLabelUtils.normalize(" 12c ")).isEqualTo("12C");
    }

    @Test
    void seatOrderSortsRowsNumerically() {
        List<String> seats = new ArrayList<>(List.of("10A", "2B", "2A", "1F"));

        seats.sort(SeatLabelUtils.SEAT_ORDER);

        assertThat(seats).containsExactly("1F", "2A", "2B", "10A");
    }

    @Test
    void capacityIsRowsTimesLetters() {
        assertThat(SeatLabelUtils.capacity(25, "ABCD")).isEqualTo(100);
    }
}
