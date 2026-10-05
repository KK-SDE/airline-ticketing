package com.airlineticketing.dto;

import java.time.LocalDate;
import java.util.List;

public record SeatMapResponse(Long flightId, String flightNumber, LocalDate flightDate, List<SeatResponse> seats) {
}
