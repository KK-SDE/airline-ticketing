package com.airlineticketing.service.impl;

import com.airlineticketing.config.AirlineProperties;
import com.airlineticketing.constants.BookingStatus;
import com.airlineticketing.constants.ErrorCode;
import com.airlineticketing.dto.BookingResponse;
import com.airlineticketing.dto.CreateBookingRequest;
import com.airlineticketing.dto.PassengerRequest;
import com.airlineticketing.entity.Aircraft;
import com.airlineticketing.entity.Booking;
import com.airlineticketing.entity.BookingSeat;
import com.airlineticketing.entity.FlightInstance;
import com.airlineticketing.exception.BadRequestException;
import com.airlineticketing.exception.ConflictException;
import com.airlineticketing.exception.NotFoundException;
import com.airlineticketing.mapper.BookingMapper;
import com.airlineticketing.repository.BookingRepository;
import com.airlineticketing.repository.BookingSeatRepository;
import com.airlineticketing.repository.FlightInstanceRepository;
import com.airlineticketing.service.BookingService;
import com.airlineticketing.util.SeatLabelUtils;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final FlightInstanceRepository flightInstanceRepository;
    private final BookingMapper bookingMapper;
    private final AirlineProperties properties;
    private final Clock clock;

    @Override
    @Transactional
    public BookingResponse create(CreateBookingRequest request) {
        List<String> seatLabels = request.passengers().stream()
                .map(PassengerRequest::seatNumber)
                .map(SeatLabelUtils::normalize)
                .toList();
        if (seatLabels.size() > properties.maxSeatsPerBooking()) {
            throw new BadRequestException(ErrorCode.TOO_MANY_SEATS,
                    "A booking can hold at most " + properties.maxSeatsPerBooking() + " seats");
        }
        if (new HashSet<>(seatLabels).size() != seatLabels.size()) {
            throw new BadRequestException(ErrorCode.DUPLICATE_SEAT_SELECTION, "Each seat can be selected only once");
        }

        FlightInstance flight = flightInstanceRepository.findByIdForUpdate(request.flightId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.FLIGHT_NOT_FOUND,
                        "Flight " + request.flightId() + " not found"));
        if (!flight.getDepartureAt().isAfter(Instant.now(clock))) {
            throw new ConflictException(ErrorCode.FLIGHT_ALREADY_DEPARTED, "Flight has already departed");
        }

        Aircraft aircraft = flight.getSchedule().getAircraft();
        List<String> invalidSeats = seatLabels.stream()
                .filter(seat -> !SeatLabelUtils.isValid(seat, aircraft.getSeatRows(), aircraft.getSeatLetters()))
                .toList();
        if (!invalidSeats.isEmpty()) {
            throw new BadRequestException(ErrorCode.INVALID_SEAT, "Seats not on this aircraft: " + invalidSeats);
        }

        List<String> takenSeats = bookingSeatRepository.findActiveSeatLabelsIn(flight.getId(), seatLabels);
        if (!takenSeats.isEmpty()) {
            throw new ConflictException(ErrorCode.SEAT_ALREADY_BOOKED, "Seats already booked: " + takenSeats);
        }

        Booking booking = bookingRepository.save(bookingMapper.toEntity(flight, request));
        List<BookingSeat> seats;
        try {
            seats = bookingSeatRepository.saveAllAndFlush(bookingMapper.toSeats(booking, request.passengers()));
        } catch (DataIntegrityViolationException e) {
            log.warn("Seat uniqueness violated while booking, flightId={}", flight.getId());
            throw new ConflictException(ErrorCode.SEAT_ALREADY_BOOKED, "One or more selected seats were just booked");
        }
        log.info("Booking created, bookingId={}, flightId={}, seats={}", booking.getId(), flight.getId(), seats.size());
        return bookingMapper.toResponse(booking, seats);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse get(UUID bookingReference) {
        Booking booking = bookingRepository.findById(bookingReference)
                .orElseThrow(() -> bookingNotFound(bookingReference));
        return bookingMapper.toResponse(booking, bookingSeatRepository.findByBookingId(bookingReference));
    }

    @Override
    @Transactional
    public BookingResponse cancel(UUID bookingReference) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingReference)
                .orElseThrow(() -> bookingNotFound(bookingReference));
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ConflictException(ErrorCode.BOOKING_ALREADY_CANCELLED,
                    "Booking " + bookingReference + " is already cancelled");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(Instant.now(clock));
        List<BookingSeat> seats = bookingSeatRepository.findByBookingId(bookingReference);
        seats.forEach(seat -> seat.setActive(false));
        log.info("Booking cancelled, bookingId={}, seatsReleased={}", bookingReference, seats.size());
        return bookingMapper.toResponse(booking, seats);
    }

    private NotFoundException bookingNotFound(UUID bookingReference) {
        return new NotFoundException(ErrorCode.BOOKING_NOT_FOUND, "Booking " + bookingReference + " not found");
    }
}
