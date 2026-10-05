package com.airlineticketing.service.impl;

import com.airlineticketing.config.AirlineProperties;
import com.airlineticketing.entity.FlightInstance;
import com.airlineticketing.entity.FlightSchedule;
import com.airlineticketing.mapper.FlightInstanceMapper;
import com.airlineticketing.repository.FlightInstanceRepository;
import com.airlineticketing.repository.FlightScheduleRepository;
import com.airlineticketing.service.FlightInstanceService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlightInstanceServiceImpl implements FlightInstanceService {

    private static final int SCHEDULE_BATCH_SIZE = 100;

    private final FlightInstanceRepository flightInstanceRepository;
    private final FlightScheduleRepository flightScheduleRepository;
    private final FlightInstanceMapper flightInstanceMapper;
    private final AirlineProperties properties;
    private final Clock clock;

    @Override
    @Transactional
    public int generateInstances(FlightSchedule schedule) {
        LocalDate from = LocalDate.now(clock);
        LocalDate to = from.plusDays(properties.bookingWindowDays());
        Set<LocalDate> existing = new HashSet<>(flightInstanceRepository.findFlightDates(schedule.getId(), from, to));

        List<FlightInstance> missing = from.datesUntil(to.plusDays(1))
                .filter(date -> schedule.getDaysOfOperation().contains(date.getDayOfWeek()))
                .filter(date -> !existing.contains(date))
                .map(date -> flightInstanceMapper.toEntity(schedule, date))
                .toList();
        flightInstanceRepository.saveAll(missing);
        return missing.size();
    }

    @Override
    @Transactional
    public int extendBookingWindow() {
        int created = 0;
        Pageable pageable = PageRequest.of(0, SCHEDULE_BATCH_SIZE, Sort.by("id"));
        Page<FlightSchedule> schedules;
        do {
            schedules = flightScheduleRepository.findAll(pageable);
            for (FlightSchedule schedule : schedules) {
                created += generateInstances(schedule);
            }
            pageable = schedules.nextPageable();
        } while (schedules.hasNext());
        return created;
    }
}
