package com.airlineticketing.mapper;

import com.airlineticketing.dto.CreateFlightScheduleRequest;
import com.airlineticketing.dto.FlightScheduleResponse;
import com.airlineticketing.entity.Aircraft;
import com.airlineticketing.entity.Airport;
import com.airlineticketing.entity.FlightSchedule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface FlightScheduleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "flightNumber", source = "request.flightNumber")
    @Mapping(target = "departureTime", source = "request.departureTime")
    @Mapping(target = "arrivalTime", source = "request.arrivalTime")
    @Mapping(target = "daysOfOperation", source = "request.daysOfOperation")
    @Mapping(target = "source", source = "source")
    @Mapping(target = "destination", source = "destination")
    @Mapping(target = "aircraft", source = "aircraft")
    FlightSchedule toEntity(CreateFlightScheduleRequest request, Airport source, Airport destination, Aircraft aircraft);

    @Mapping(target = "sourceAirportCode", source = "source.code")
    @Mapping(target = "destinationAirportCode", source = "destination.code")
    @Mapping(target = "aircraftId", source = "aircraft.id")
    @Mapping(target = "aircraftType", source = "aircraft.aircraftType")
    @Mapping(target = "daysOfOperation", expression = "java(new java.util.TreeSet<>(schedule.getDaysOfOperation()))")
    FlightScheduleResponse toResponse(FlightSchedule schedule);
}
