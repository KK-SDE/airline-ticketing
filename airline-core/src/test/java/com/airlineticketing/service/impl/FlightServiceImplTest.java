package com.airlineticketing.service.impl;

import static com.airlineticketing.testutil.TestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.airlineticketing.config.AirlineProperties;
import com.airlineticketing.constants.ErrorCode;
import com.airlineticketing.constants.SeatStatus;
import com.airlineticketing.dto.FlightSearchResponse;
import com.airlineticketing.dto.FlightSeatCount;
import com.airlineticketing.dto.SeatMapResponse;
import com.airlineticketing.dto.SeatResponse;
import com.airlineticketing.entity.FlightInstance;
import com.airlineticketing.exception.ApiException;
import com.airlineticketing.exception.BadRequestException;
import com.airlineticketing.exception.NotFoundException;
import com.airlineticketing.mapper.FlightMapperImpl;
import com.airlineticketing.repository.BookingSeatRepository;
import com.airlineticketing.repository.FlightInstanceRepository;
import com.airlineticketing.testutil.TestData;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class FlightServiceImplTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 4);

    @Mock
    private FlightInstanceRepository flightInstanceRepository;
    @Mock
    private BookingSeatRepository bookingSeatRepository;

    private FlightServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FlightServiceImpl(flightInstanceRepository, bookingSeatRepository, new FlightMapperImpl(),
                new AirlineProperties(365, 9), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void searchReturnsAvailableSeatsAsCapacityMinusBooked() {
        FlightInstance flight = TestData.futureFlight();
        when(flightInstanceRepository.search(eq("DXB"), eq("LHR"), eq(TODAY.plusDays(1)), eq(NOW), any()))
                .thenReturn(new PageImpl<>(List.of(flight)));
        when(bookingSeatRepository.countActiveByFlightIds(List.of(100L))).thenReturn(List.of(new FlightSeatCount(100L, 12)));

        Page<FlightSearchResponse> result = service.search("dxb", "lhr", TODAY.plusDays(1), PageRequest.of(0, 20));

        assertThat(result.getContent()).singleElement().satisfies(response -> {
            assertThat(response.flightNumber()).isEqualTo("XY101");
            assertThat(response.availableSeats()).isEqualTo(168);
            assertThat(response.departureTime()).isEqualTo(flight.getDepartureAt());
        });
    }

    @Test
    void searchWithNoFlightsSkipsSeatCountQuery() {
        when(flightInstanceRepository.search(any(), any(), any(), any(), any())).thenReturn(Page.empty());

        assertThat(service.search("DXB", "LHR", TODAY, PageRequest.of(0, 20))).isEmpty();
        verify(bookingSeatRepository, never()).countActiveByFlightIds(any());
    }

    @Test
    void searchRejectsPastAndBeyondWindowDates() {
        assertThatThrownBy(() -> service.search("DXB", "LHR", TODAY.minusDays(1), PageRequest.of(0, 20)))
                .isInstanceOf(BadRequestException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.TRAVEL_DATE_OUT_OF_WINDOW));
        assertThatThrownBy(() -> service.search("DXB", "LHR", TODAY.plusDays(366), PageRequest.of(0, 20)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void searchAcceptsTheLastDayOfTheWindow() {
        when(flightInstanceRepository.search(any(), any(), eq(TODAY.plusDays(365)), any(), any())).thenReturn(Page.empty());

        assertThat(service.search("DXB", "LHR", TODAY.plusDays(365), PageRequest.of(0, 20))).isEmpty();
    }

    @Test
    void seatMapMarksBookedSeats() {
        FlightInstance flight = TestData.futureFlight();
        when(flightInstanceRepository.findWithDetailsById(100L)).thenReturn(Optional.of(flight));
        when(bookingSeatRepository.findActiveSeatLabels(100L)).thenReturn(List.of("1B", "30F"));

        SeatMapResponse seatMap = service.getSeatMap(100L);

        assertThat(seatMap.seats()).hasSize(180);
        assertThat(seatMap.seats()).filteredOn(seat -> seat.status() == SeatStatus.BOOKED)
                .extracting(SeatResponse::seatNumber).containsExactly("1B", "30F");
        assertThat(seatMap.seats().get(0)).isEqualTo(new SeatResponse("1A", SeatStatus.AVAILABLE));
    }

    @Test
    void seatMapFailsForUnknownFlight() {
        when(flightInstanceRepository.findWithDetailsById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSeatMap(1L)).isInstanceOf(NotFoundException.class);
    }
}
