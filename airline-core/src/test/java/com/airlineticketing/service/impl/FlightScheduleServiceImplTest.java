package com.airlineticketing.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.airlineticketing.constants.ErrorCode;
import com.airlineticketing.dto.CreateFlightScheduleRequest;
import com.airlineticketing.dto.FlightScheduleResponse;
import com.airlineticketing.entity.FlightSchedule;
import com.airlineticketing.exception.ApiException;
import com.airlineticketing.exception.BadRequestException;
import com.airlineticketing.exception.ConflictException;
import com.airlineticketing.exception.NotFoundException;
import com.airlineticketing.mapper.FlightScheduleMapperImpl;
import com.airlineticketing.repository.AircraftRepository;
import com.airlineticketing.repository.AirportRepository;
import com.airlineticketing.repository.FlightScheduleRepository;
import com.airlineticketing.service.FlightInstanceService;
import com.airlineticketing.testutil.TestData;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlightScheduleServiceImplTest {

    @Mock
    private FlightScheduleRepository flightScheduleRepository;
    @Mock
    private AirportRepository airportRepository;
    @Mock
    private AircraftRepository aircraftRepository;
    @Mock
    private FlightInstanceService flightInstanceService;

    private FlightScheduleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FlightScheduleServiceImpl(flightScheduleRepository, airportRepository, aircraftRepository,
                flightInstanceService, new FlightScheduleMapperImpl());
    }

    @Test
    void createSavesScheduleAndGeneratesInstances() {
        when(flightScheduleRepository.existsByFlightNumber("XY101")).thenReturn(false);
        when(airportRepository.findById("DXB")).thenReturn(Optional.of(TestData.airport("DXB")));
        when(airportRepository.findById("LHR")).thenReturn(Optional.of(TestData.airport("LHR")));
        when(aircraftRepository.findById(1L)).thenReturn(Optional.of(TestData.aircraft()));
        when(flightScheduleRepository.save(any(FlightSchedule.class))).thenAnswer(invocation -> {
            FlightSchedule schedule = invocation.getArgument(0);
            schedule.setId(7L);
            return schedule;
        });

        FlightScheduleResponse response = service.create(request("XY101", "dxb", "lhr"));

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.sourceAirportCode()).isEqualTo("DXB");
        assertThat(response.daysOfOperation()).containsExactly(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        verify(flightInstanceService).generateInstances(any(FlightSchedule.class));
    }

    @Test
    void createRejectsSameSourceAndDestination() {
        assertThatThrownBy(() -> service.create(request("XY101", "DXB", "dxb")))
                .isInstanceOf(BadRequestException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.SAME_SOURCE_AND_DESTINATION));
    }

    @Test
    void createRejectsDuplicateFlightNumber() {
        when(flightScheduleRepository.existsByFlightNumber("XY101")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("XY101", "DXB", "LHR")))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.FLIGHT_NUMBER_ALREADY_EXISTS));
        verify(flightScheduleRepository, never()).save(any());
    }

    @Test
    void createFailsForUnknownAirport() {
        when(airportRepository.findById("DXB")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request("XY101", "DXB", "LHR")))
                .isInstanceOf(NotFoundException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.AIRPORT_NOT_FOUND));
    }

    @Test
    void createFailsForUnknownAircraft() {
        when(airportRepository.findById("DXB")).thenReturn(Optional.of(TestData.airport("DXB")));
        when(airportRepository.findById("LHR")).thenReturn(Optional.of(TestData.airport("LHR")));
        when(aircraftRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request("XY101", "DXB", "LHR")))
                .isInstanceOf(NotFoundException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.AIRCRAFT_NOT_FOUND));
    }

    @Test
    void getFailsForUnknownSchedule() {
        when(flightScheduleRepository.findWithDetailsById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(5L)).isInstanceOf(NotFoundException.class);
    }

    private static CreateFlightScheduleRequest request(String flightNumber, String source, String destination) {
        return new CreateFlightScheduleRequest(flightNumber, source, destination, LocalTime.of(9, 30),
                LocalTime.of(13, 45), 1L, Set.of(DayOfWeek.FRIDAY, DayOfWeek.MONDAY));
    }
}
