package com.airlineticketing.service;

import com.airlineticketing.dto.FlightSearchResponse;
import com.airlineticketing.dto.SeatMapResponse;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FlightService {

    Page<FlightSearchResponse> search(String source, String destination, LocalDate date, Pageable pageable);

    SeatMapResponse getSeatMap(Long flightId);
}
