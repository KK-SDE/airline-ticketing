package com.airlineticketing.mapper;

import com.airlineticketing.entity.FlightInstance;
import com.airlineticketing.entity.FlightSchedule;
import com.airlineticketing.util.FlightTimeUtils;
import java.time.LocalDate;
import org.mapstruct.Mapper;

@Mapper
public interface FlightInstanceMapper {

    default FlightInstance toEntity(FlightSchedule schedule, LocalDate flightDate) {
        return FlightInstance.builder()
                .schedule(schedule)
                .flightDate(flightDate)
                .departureAt(FlightTimeUtils.departureAt(flightDate, schedule.getDepartureTime()))
                .arrivalAt(FlightTimeUtils.arrivalAt(flightDate, schedule.getDepartureTime(), schedule.getArrivalTime()))
                .build();
    }
}
