package com.airlineticketing.controller;

import com.airlineticketing.dto.CreateFlightScheduleRequest;
import com.airlineticketing.dto.FlightScheduleResponse;
import com.airlineticketing.dto.PageResponse;
import com.airlineticketing.service.FlightScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/flight-schedules")
@RequiredArgsConstructor
public class AdminFlightScheduleController {

    private final FlightScheduleService flightScheduleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FlightScheduleResponse create(@Valid @RequestBody CreateFlightScheduleRequest request) {
        return flightScheduleService.create(request);
    }

    @GetMapping("/{id}")
    public FlightScheduleResponse getFlightSchedule(@PathVariable Long id) {
        return flightScheduleService.get(id);
    }

    @GetMapping
    public PageResponse<FlightScheduleResponse> list(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return PageResponse.of(flightScheduleService.list(pageable));
    }
}
