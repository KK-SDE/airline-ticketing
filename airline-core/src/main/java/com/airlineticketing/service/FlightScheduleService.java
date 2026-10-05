package com.airlineticketing.service;

import com.airlineticketing.dto.CreateFlightScheduleRequest;
import com.airlineticketing.dto.FlightScheduleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FlightScheduleService {

    FlightScheduleResponse create(CreateFlightScheduleRequest request);

    FlightScheduleResponse get(Long id);

    Page<FlightScheduleResponse> list(Pageable pageable);
}
