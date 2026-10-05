package com.airlineticketing.service.impl;

import com.airlineticketing.config.AirlineProperties;
import com.airlineticketing.constants.ErrorCode;
import com.airlineticketing.dto.FlightSearchResponse;
import com.airlineticketing.dto.FlightSeatCount;
import com.airlineticketing.dto.SeatMapResponse;
import com.airlineticketing.entity.Aircraft;
import com.airlineticketing.entity.FlightInstance;
import com.airlineticketing.exception.BadRequestException;
import com.airlineticketing.exception.NotFoundException;
import com.airlineticketing.mapper.FlightMapper;
import com.airlineticketing.repository.BookingSeatRepository;
import com.airlineticketing.repository.FlightInstanceRepository;
import com.airlineticketing.service.FlightService;
import com.airlineticketing.util.SeatLabelUtils;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlightServiceImpl implements FlightService {

    private final FlightInstanceRepository flightInstanceRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final FlightMapper flightMapper;
    private final AirlineProperties properties;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public Page<FlightSearchResponse> search(String source, String destination, LocalDate date, Pageable pageable) {
        LocalDate today = LocalDate.now(clock);
        if (date.isBefore(today) || date.isAfter(today.plusDays(properties.bookingWindowDays()))) {
            throw new BadRequestException(ErrorCode.TRAVEL_DATE_OUT_OF_WINDOW,
                    "Travel date must be between " + today + " and " + today.plusDays(properties.bookingWindowDays()));
        }
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<FlightInstance> flights = flightInstanceRepository.search(
                source.toUpperCase(Locale.ROOT), destination.toUpperCase(Locale.ROOT), date,
                Instant.now(clock), unsorted);

        List<Long> flightIds = flights.map(FlightInstance::getId).getContent();
        Map<Long, Long> bookedByFlight = flightIds.isEmpty() ? Map.of()
                : bookingSeatRepository.countActiveByFlightIds(flightIds).stream()
                        .collect(Collectors.toMap(FlightSeatCount::flightId, FlightSeatCount::bookedSeats));

        return flights.map(flight -> {
            Aircraft aircraft = flight.getSchedule().getAircraft();
            int capacity = SeatLabelUtils.capacity(aircraft.getSeatRows(), aircraft.getSeatLetters());
            long booked = bookedByFlight.getOrDefault(flight.getId(), 0L);
            return flightMapper.toSearchResponse(flight, (int) (capacity - booked));
        });
    }

    @Override
    @Transactional(readOnly = true)
    public SeatMapResponse getSeatMap(Long flightId) {
        FlightInstance flight = flightInstanceRepository.findWithDetailsById(flightId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.FLIGHT_NOT_FOUND,
                        "Flight " + flightId + " not found"));
        Aircraft aircraft = flight.getSchedule().getAircraft();
        List<String> allSeats = SeatLabelUtils.allLabels(aircraft.getSeatRows(), aircraft.getSeatLetters());
        return flightMapper.toSeatMap(flight, allSeats, new HashSet<>(bookingSeatRepository.findActiveSeatLabels(flightId)));
    }
}
