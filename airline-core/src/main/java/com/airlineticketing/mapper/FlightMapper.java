package com.airlineticketing.mapper;

import com.airlineticketing.constants.SeatStatus;
import com.airlineticketing.dto.FlightSearchResponse;
import com.airlineticketing.dto.SeatMapResponse;
import com.airlineticketing.dto.SeatResponse;
import com.airlineticketing.entity.FlightInstance;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface FlightMapper {

    @Mapping(target = "flightId", source = "flight.id")
    @Mapping(target = "flightNumber", source = "flight.schedule.flightNumber")
    @Mapping(target = "sourceAirportCode", source = "flight.schedule.source.code")
    @Mapping(target = "destinationAirportCode", source = "flight.schedule.destination.code")
    @Mapping(target = "flightDate", source = "flight.flightDate")
    @Mapping(target = "departureTime", source = "flight.departureAt")
    @Mapping(target = "arrivalTime", source = "flight.arrivalAt")
    @Mapping(target = "availableSeats", source = "availableSeats")
    FlightSearchResponse toSearchResponse(FlightInstance flight, int availableSeats);

    default SeatMapResponse toSeatMap(FlightInstance flight, List<String> allSeats, Set<String> bookedSeats) {
        List<SeatResponse> seats = allSeats.stream()
                .map(seat -> new SeatResponse(seat, bookedSeats.contains(seat) ? SeatStatus.BOOKED : SeatStatus.AVAILABLE))
                .toList();
        return new SeatMapResponse(flight.getId(), flight.getSchedule().getFlightNumber(), flight.getFlightDate(), seats);
    }
}
