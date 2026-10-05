package com.airlineticketing.dto;

import com.airlineticketing.constants.SeatStatus;

public record SeatResponse(String seatNumber, SeatStatus status) {
}
