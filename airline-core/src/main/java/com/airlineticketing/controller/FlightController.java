package com.airlineticketing.controller;

import com.airlineticketing.dto.FlightSearchResponse;
import com.airlineticketing.dto.PageResponse;
import com.airlineticketing.dto.SeatMapResponse;
import com.airlineticketing.service.FlightService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    @GetMapping("/search")
    public PageResponse<FlightSearchResponse> search(
            @RequestParam @NotNull @Pattern(regexp = "^[A-Za-z]{3}$", message = "must be a 3-letter IATA code") String source,
            @RequestParam @NotNull @Pattern(regexp = "^[A-Za-z]{3}$", message = "must be a 3-letter IATA code") String destination,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(flightService.search(source, destination, date, pageable));
    }

    @GetMapping("/{flightId}/seats")
    public SeatMapResponse seatMap(@PathVariable Long flightId) {
        return flightService.getSeatMap(flightId);
    }
}
