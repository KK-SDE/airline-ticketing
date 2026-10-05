package com.airlineticketing.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "airline")
public record AirlineProperties(
        @DefaultValue("365") @Min(1) @Max(730) int bookingWindowDays,
        @DefaultValue("9") @Min(1) @Max(20) int maxSeatsPerBooking) {
}
