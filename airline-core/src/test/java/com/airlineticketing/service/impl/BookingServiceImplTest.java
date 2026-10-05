package com.airlineticketing.service.impl;

import static com.airlineticketing.testutil.TestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.airlineticketing.config.AirlineProperties;
import com.airlineticketing.constants.BookingStatus;
import com.airlineticketing.constants.ErrorCode;
import com.airlineticketing.dto.BookingResponse;
import com.airlineticketing.dto.CreateBookingRequest;
import com.airlineticketing.dto.PassengerRequest;
import com.airlineticketing.entity.Booking;
import com.airlineticketing.entity.BookingSeat;
import com.airlineticketing.entity.FlightInstance;
import com.airlineticketing.exception.ApiException;
import com.airlineticketing.exception.BadRequestException;
import com.airlineticketing.exception.ConflictException;
import com.airlineticketing.exception.NotFoundException;
import com.airlineticketing.mapper.BookingMapperImpl;
import com.airlineticketing.repository.BookingRepository;
import com.airlineticketing.repository.BookingSeatRepository;
import com.airlineticketing.repository.FlightInstanceRepository;
import com.airlineticketing.testutil.TestData;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingSeatRepository bookingSeatRepository;
    @Mock
    private FlightInstanceRepository flightInstanceRepository;

    private BookingServiceImpl service;
    private FlightInstance flight;

    @BeforeEach
    void setUp() {
        service = new BookingServiceImpl(bookingRepository, bookingSeatRepository, flightInstanceRepository,
                new BookingMapperImpl(), new AirlineProperties(365, 3), Clock.fixed(NOW, ZoneOffset.UTC));
        flight = TestData.futureFlight();
    }

    @Test
    void createAssignsRequestedSeatsAndConfirmsBooking() {
        givenFlightLocked();
        when(bookingSeatRepository.findActiveSeatLabelsIn(eq(100L), anyCollection())).thenReturn(List.of());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingSeatRepository.saveAllAndFlush(anyCollection())).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = service.create(request("1b", "1A"));

        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(response.flightNumber()).isEqualTo("XY101");
        assertThat(response.passengerCount()).isEqualTo(2);
        assertThat(response.assignedSeats()).extracting("seatNumber").containsExactly("1A", "1B");
    }

    @Test
    void createRejectsSeatsThatAreAlreadyBooked() {
        givenFlightLocked();
        when(bookingSeatRepository.findActiveSeatLabelsIn(eq(100L), anyCollection())).thenReturn(List.of("1A"));

        assertThatThrownBy(() -> service.create(request("1A", "1B")))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.SEAT_ALREADY_BOOKED));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createTranslatesUniqueIndexViolationIntoSeatConflict() {
        givenFlightLocked();
        when(bookingSeatRepository.findActiveSeatLabelsIn(eq(100L), anyCollection())).thenReturn(List.of());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingSeatRepository.saveAllAndFlush(anyCollection())).thenThrow(new DataIntegrityViolationException("uq"));

        assertThatThrownBy(() -> service.create(request("1A")))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.SEAT_ALREADY_BOOKED));
    }

    @Test
    void createRejectsSeatsOutsideAircraftConfiguration() {
        givenFlightLocked();

        assertThatThrownBy(() -> service.create(request("31A")))
                .isInstanceOf(BadRequestException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_SEAT));
    }

    @Test
    void createRejectsDuplicateSeatsInRequest() {
        assertThatThrownBy(() -> service.create(request("2A", "2a")))
                .isInstanceOf(BadRequestException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_SEAT_SELECTION));
        verify(flightInstanceRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void createRejectsMoreSeatsThanAllowed() {
        assertThatThrownBy(() -> service.create(request("1A", "1B", "1C", "1D")))
                .isInstanceOf(BadRequestException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.TOO_MANY_SEATS));
    }

    @Test
    void createFailsForUnknownFlight() {
        when(flightInstanceRepository.findByIdForUpdate(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request("1A")))
                .isInstanceOf(NotFoundException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.FLIGHT_NOT_FOUND));
    }

    @Test
    void createFailsForDepartedFlight() {
        flight = TestData.flight(NOW.minusSeconds(60));
        givenFlightLocked();

        assertThatThrownBy(() -> service.create(request("1A")))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.FLIGHT_ALREADY_DEPARTED));
    }

    @Test
    void cancelMarksBookingCancelledAndReleasesSeats() {
        UUID id = UUID.randomUUID();
        Booking booking = confirmedBooking(id);
        BookingSeat seat = BookingSeat.builder().booking(booking).flightInstance(flight)
                .seatLabel("1A").passengerName("Alice").active(true).build();
        when(bookingRepository.findByIdForUpdate(id)).thenReturn(Optional.of(booking));
        when(bookingSeatRepository.findByBookingId(id)).thenReturn(List.of(seat));

        BookingResponse response = service.cancel(id);

        assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getCancelledAt()).isEqualTo(NOW);
        assertThat(seat.isActive()).isFalse();
    }

    @Test
    void cancelRejectsAlreadyCancelledBooking() {
        UUID id = UUID.randomUUID();
        Booking booking = confirmedBooking(id);
        booking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findByIdForUpdate(id)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.cancel(id))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.BOOKING_ALREADY_CANCELLED));
    }

    @Test
    void cancelFailsForUnknownBooking() {
        UUID id = UUID.randomUUID();
        when(bookingRepository.findByIdForUpdate(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(id))
                .isInstanceOf(NotFoundException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.BOOKING_NOT_FOUND));
    }

    @Test
    void getFailsForUnknownBooking() {
        UUID id = UUID.randomUUID();
        when(bookingRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id)).isInstanceOf(NotFoundException.class);
    }

    private void givenFlightLocked() {
        when(flightInstanceRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(flight));
    }

    private Booking confirmedBooking(UUID id) {
        Booking booking = Booking.builder().flightInstance(flight).status(BookingStatus.CONFIRMED).passengerCount(1).build();
        booking.setId(id);
        return booking;
    }

    private static CreateBookingRequest request(String... seats) {
        List<PassengerRequest> passengers = java.util.Arrays.stream(seats)
                .map(seat -> new PassengerRequest("Passenger " + seat, seat))
                .toList();
        return new CreateBookingRequest(100L, passengers);
    }
}
