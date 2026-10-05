package com.airlineticketing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateBookingRequest(
        @NotNull Long flightId,
        @NotEmpty List<@NotNull @Valid PassengerRequest> passengers) {
}
