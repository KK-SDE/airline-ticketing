package com.airlineticketing.service.impl;

import static com.airlineticketing.testutil.TestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.airlineticketing.config.AirlineProperties;
import com.airlineticketing.entity.FlightInstance;
import com.airlineticketing.entity.FlightSchedule;
import com.airlineticketing.mapper.FlightInstanceMapperImpl;
import com.airlineticketing.repository.FlightInstanceRepository;
import com.airlineticketing.repository.FlightScheduleRepository;
import com.airlineticketing.testutil.TestData;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class FlightInstanceServiceImplTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 4);

    @Mock
    private FlightInstanceRepository flightInstanceRepository;
    @Mock
    private FlightScheduleRepository flightScheduleRepository;
    @Captor
    private ArgumentCaptor<List<FlightInstance>> savedInstances;

    private FlightInstanceServiceImpl service;
    private FlightSchedule schedule;

    @BeforeEach
    void setUp() {
        service = new FlightInstanceServiceImpl(flightInstanceRepository, flightScheduleRepository,
                new FlightInstanceMapperImpl(), new AirlineProperties(365, 9), Clock.fixed(NOW, ZoneOffset.UTC));
        schedule = TestData.schedule();
    }

    @Test
    void generatesOnlyOperatingDaysWithinTheWindow() {
        int created = service.generateInstances(schedule);

        verify(flightInstanceRepository).saveAll(savedInstances.capture());
        List<FlightInstance> instances = savedInstances.getValue();
        assertThat(created).isEqualTo(instances.size()).isEqualTo(157);
        assertThat(instances).extracting(FlightInstance::getFlightDate)
                .allMatch(date -> !date.isBefore(TODAY) && !date.isAfter(TODAY.plusDays(365)))
                .allMatch(date -> schedule.getDaysOfOperation().contains(date.getDayOfWeek()));
        assertThat(instances.get(0).getFlightDate()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(instances.get(0).getFlightDate().getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    void skipsDatesThatAlreadyExist() {
        when(flightInstanceRepository.findFlightDates(eq(10L), any(), any()))
                .thenReturn(List.of(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 7)));

        int created = service.generateInstances(schedule);

        assertThat(created).isEqualTo(155);
    }

    @Test
    void extendBookingWindowProcessesEverySchedule() {
        when(flightScheduleRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(schedule)));

        int created = service.extendBookingWindow();

        assertThat(created).isEqualTo(157);
        verify(flightInstanceRepository).saveAll(anyList());
    }
}
