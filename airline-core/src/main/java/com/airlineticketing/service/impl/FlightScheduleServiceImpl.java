package com.airlineticketing.service.impl;

import com.airlineticketing.constants.ErrorCode;
import com.airlineticketing.dto.CreateFlightScheduleRequest;
import com.airlineticketing.dto.FlightScheduleResponse;
import com.airlineticketing.entity.Aircraft;
import com.airlineticketing.entity.Airport;
import com.airlineticketing.entity.FlightSchedule;
import com.airlineticketing.exception.BadRequestException;
import com.airlineticketing.exception.ConflictException;
import com.airlineticketing.exception.NotFoundException;
import com.airlineticketing.mapper.FlightScheduleMapper;
import com.airlineticketing.repository.AircraftRepository;
import com.airlineticketing.repository.AirportRepository;
import com.airlineticketing.repository.FlightScheduleRepository;
import com.airlineticketing.service.FlightInstanceService;
import com.airlineticketing.service.FlightScheduleService;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlightScheduleServiceImpl implements FlightScheduleService {

    private final FlightScheduleRepository flightScheduleRepository;
    private final AirportRepository airportRepository;
    private final AircraftRepository aircraftRepository;
    private final FlightInstanceService flightInstanceService;
    private final FlightScheduleMapper flightScheduleMapper;

    @Override
    @Transactional
    public FlightScheduleResponse create(CreateFlightScheduleRequest request) {
        String sourceCode = request.sourceAirportCode().toUpperCase(Locale.ROOT);
        String destinationCode = request.destinationAirportCode().toUpperCase(Locale.ROOT);
        if (sourceCode.equals(destinationCode)) {
            throw new BadRequestException(ErrorCode.SAME_SOURCE_AND_DESTINATION,
                    "Source and destination airports must differ");
        }
        if (flightScheduleRepository.existsByFlightNumber(request.flightNumber())) {
            throw new ConflictException(ErrorCode.FLIGHT_NUMBER_ALREADY_EXISTS,
                    "Flight number " + request.flightNumber() + " already has a schedule");
        }
        Airport source = findAirport(sourceCode);
        Airport destination = findAirport(destinationCode);
        Aircraft aircraft = aircraftRepository.findById(request.aircraftId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.AIRCRAFT_NOT_FOUND,
                        "Aircraft " + request.aircraftId() + " not found"));

        FlightSchedule schedule = flightScheduleRepository.save(
                flightScheduleMapper.toEntity(request, source, destination, aircraft));
        int instances = flightInstanceService.generateInstances(schedule);
        log.info("Flight schedule created, scheduleId={}, flightNumber={}, flightInstances={}",
                schedule.getId(), schedule.getFlightNumber(), instances);
        return flightScheduleMapper.toResponse(schedule);
    }

    @Override
    @Transactional(readOnly = true)
    public FlightScheduleResponse get(Long id) {
        return flightScheduleRepository.findWithDetailsById(id)
                .map(flightScheduleMapper::toResponse)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SCHEDULE_NOT_FOUND,
                        "Flight schedule " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FlightScheduleResponse> list(Pageable pageable) {
        return flightScheduleRepository.findAll(pageable).map(flightScheduleMapper::toResponse);
    }

    private Airport findAirport(String code) {
        return airportRepository.findById(code)
                .orElseThrow(() -> new NotFoundException(ErrorCode.AIRPORT_NOT_FOUND,
                        "Airport " + code + " not found"));
    }
}
