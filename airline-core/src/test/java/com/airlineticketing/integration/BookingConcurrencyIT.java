package com.airlineticketing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.airlineticketing.constants.BookingStatus;
import com.airlineticketing.constants.SeatStatus;
import com.airlineticketing.dto.BookingResponse;
import com.airlineticketing.dto.CreateBookingRequest;
import com.airlineticketing.dto.CreateFlightScheduleRequest;
import com.airlineticketing.dto.FlightSearchResponse;
import com.airlineticketing.dto.PassengerRequest;
import com.airlineticketing.dto.SeatMapResponse;
import com.airlineticketing.dto.SeatResponse;
import com.airlineticketing.exception.ConflictException;
import com.airlineticketing.service.BookingService;
import com.airlineticketing.service.FlightScheduleService;
import com.airlineticketing.service.FlightService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

class BookingConcurrencyIT extends PostgresIntegrationTest {

    private static final int THREADS = 20;
    private static final AtomicInteger FLIGHT_SEQUENCE = new AtomicInteger(100);

    @Autowired
    private FlightScheduleService flightScheduleService;
    @Autowired
    private FlightService flightService;
    @Autowired
    private BookingService bookingService;

    private Long flightId;

    @BeforeEach
    void createFlight() {
        String flightNumber = "XY" + FLIGHT_SEQUENCE.incrementAndGet();
        LocalDate flightDate = LocalDate.now(ZoneOffset.UTC).plusDays(2);
        flightScheduleService.create(new CreateFlightScheduleRequest(flightNumber, "DXB", "LHR",
                LocalTime.of(9, 30), LocalTime.of(13, 45), 1L, EnumSet.allOf(DayOfWeek.class)));
        flightId = flightService.search("DXB", "LHR", flightDate, PageRequest.of(0, 100)).getContent().stream()
                .filter(flight -> flight.flightNumber().equals(flightNumber))
                .map(FlightSearchResponse::flightId)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void onlyOneOfManyConcurrentBookingsWinsTheSameSeat() throws Exception {
        List<Callable<BookingResponse>> tasks = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            tasks.add(bookingTask("Passenger " + i, "12C"));
        }

        List<Object> outcomes = runConcurrently(tasks);

        assertThat(outcomes).filteredOn(BookingResponse.class::isInstance).hasSize(1);
        assertThat(outcomes).filteredOn(ConflictException.class::isInstance).hasSize(THREADS - 1);
        assertThat(bookedSeats()).containsExactly("12C");
    }

    @Test
    void concurrentBookingsOfDifferentSeatsAllSucceed() throws Exception {
        List<Callable<BookingResponse>> tasks = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            tasks.add(bookingTask("Passenger " + i, (i + 1) + "A"));
        }

        List<Object> outcomes = runConcurrently(tasks);

        assertThat(outcomes).allMatch(BookingResponse.class::isInstance);
        assertThat(bookedSeats()).hasSize(THREADS);
    }

    @Test
    void overlappingMultiSeatBookingsNeverShareASeat() throws Exception {
        List<Callable<BookingResponse>> tasks = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            tasks.add(i % 2 == 0 ? bookingTask("P" + i, "20A", "20B") : bookingTask("P" + i, "20B", "20A"));
        }

        List<Object> outcomes = runConcurrently(tasks);

        assertThat(outcomes).filteredOn(BookingResponse.class::isInstance).hasSize(1);
        assertThat(bookedSeats()).containsExactlyInAnyOrder("20A", "20B");
    }

    @Test
    void cancellingReleasesSeatsAndOnlyOneConcurrentCancelSucceeds() throws Exception {
        BookingResponse booking = bookingService.create(request(passenger("Alice", "5A"), passenger("Bob", "5B")));
        assertThat(bookedSeats()).containsExactlyInAnyOrder("5A", "5B");

        List<Callable<BookingResponse>> cancels = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            cancels.add(() -> bookingService.cancel(booking.bookingReference()));
        }
        List<Object> outcomes = runConcurrently(cancels);

        assertThat(outcomes).filteredOn(BookingResponse.class::isInstance).hasSize(1);
        assertThat(outcomes).filteredOn(ConflictException.class::isInstance).hasSize(9);
        assertThat(bookedSeats()).isEmpty();
        assertThat(bookingService.get(booking.bookingReference()).status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(bookingService.create(request(passenger("Carol", "5A"))).status()).isEqualTo(BookingStatus.CONFIRMED);
    }

    private Callable<BookingResponse> bookingTask(String name, String... seats) {
        PassengerRequest[] passengers = new PassengerRequest[seats.length];
        for (int i = 0; i < seats.length; i++) {
            passengers[i] = passenger(name + "-" + seats[i], seats[i]);
        }
        return () -> bookingService.create(request(passengers));
    }

    private CreateBookingRequest request(PassengerRequest... passengers) {
        return new CreateBookingRequest(flightId, List.of(passengers));
    }

    private static PassengerRequest passenger(String name, String seat) {
        return new PassengerRequest(name, seat);
    }

    private List<String> bookedSeats() {
        SeatMapResponse seatMap = flightService.getSeatMap(flightId);
        return seatMap.seats().stream()
                .filter(seat -> seat.status() == SeatStatus.BOOKED)
                .map(SeatResponse::seatNumber)
                .toList();
    }

    private List<Object> runConcurrently(List<? extends Callable<BookingResponse>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (Callable<BookingResponse> task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        return task.call();
                    } catch (RuntimeException e) {
                        return e;
                    }
                }));
            }
            ready.await();
            start.countDown();
            List<Object> outcomes = new ArrayList<>();
            for (Future<Object> future : futures) {
                outcomes.add(future.get());
            }
            return outcomes;
        } finally {
            executor.shutdownNow();
        }
    }
}
