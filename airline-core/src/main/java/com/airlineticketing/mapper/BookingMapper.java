package com.airlineticketing.mapper;

import com.airlineticketing.constants.BookingStatus;
import com.airlineticketing.dto.BookingResponse;
import com.airlineticketing.dto.BookingSeatResponse;
import com.airlineticketing.dto.CreateBookingRequest;
import com.airlineticketing.dto.PassengerRequest;
import com.airlineticketing.entity.Booking;
import com.airlineticketing.entity.BookingSeat;
import com.airlineticketing.entity.FlightInstance;
import com.airlineticketing.util.SeatLabelUtils;
import java.util.Comparator;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper
public interface BookingMapper {

    default Booking toEntity(FlightInstance flight, CreateBookingRequest request) {
        return Booking.builder()
                .flightInstance(flight)
                .status(BookingStatus.CONFIRMED)
                .passengerCount(request.passengers().size())
                .build();
    }

    default List<BookingSeat> toSeats(Booking booking, List<PassengerRequest> passengers) {
        return passengers.stream()
                .map(passenger -> BookingSeat.builder()
                        .booking(booking)
                        .flightInstance(booking.getFlightInstance())
                        .seatLabel(SeatLabelUtils.normalize(passenger.seatNumber()))
                        .passengerName(passenger.name().trim())
                        .active(true)
                        .build())
                .toList();
    }

    default BookingResponse toResponse(Booking booking, List<BookingSeat> seats) {
        FlightInstance flight = booking.getFlightInstance();
        List<BookingSeatResponse> assignedSeats = seats.stream()
                .sorted(Comparator.comparing(BookingSeat::getSeatLabel, SeatLabelUtils.SEAT_ORDER))
                .map(seat -> new BookingSeatResponse(seat.getSeatLabel(), seat.getPassengerName()))
                .toList();
        return new BookingResponse(booking.getId(), flight.getSchedule().getFlightNumber(), flight.getFlightDate(),
                booking.getPassengerCount(), assignedSeats, booking.getStatus());
    }
}
