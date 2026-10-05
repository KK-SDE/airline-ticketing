package com.airlineticketing.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.airlineticketing.constants.ErrorCode;
import com.airlineticketing.constants.SeatStatus;
import com.airlineticketing.dto.FlightSearchResponse;
import com.airlineticketing.dto.SeatMapResponse;
import com.airlineticketing.dto.SeatResponse;
import com.airlineticketing.exception.NotFoundException;
import com.airlineticketing.service.FlightService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FlightController.class)
class FlightControllerTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 5);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FlightService flightService;

    @Test
    void searchReturnsPagedFlights() throws Exception {
        FlightSearchResponse flight = new FlightSearchResponse(1L, "XY101", "DXB", "LHR", DATE,
                Instant.parse("2026-10-05T09:30:00Z"), Instant.parse("2026-10-05T13:45:00Z"), 180);
        when(flightService.search(eq("DXB"), eq("LHR"), eq(DATE), any())).thenReturn(new PageImpl<>(List.of(flight)));

        mockMvc.perform(get("/api/v1/flights/search").param("source", "DXB").param("destination", "LHR")
                        .param("date", "2026-10-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].flightNumber").value("XY101"))
                .andExpect(jsonPath("$.content[0].availableSeats").value(180))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void searchRejectsInvalidAirportCode() throws Exception {
        mockMvc.perform(get("/api/v1/flights/search").param("source", "DUBAI").param("destination", "LHR")
                        .param("date", "2026-10-05"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("source"));
    }

    @Test
    void searchRejectsMissingAndMalformedParameters() throws Exception {
        mockMvc.perform(get("/api/v1/flights/search").param("source", "DXB").param("destination", "LHR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("date"));
        mockMvc.perform(get("/api/v1/flights/search").param("source", "DXB").param("destination", "LHR")
                        .param("date", "05-10-2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void seatMapReturnsSeats() throws Exception {
        when(flightService.getSeatMap(1L)).thenReturn(new SeatMapResponse(1L, "XY101", DATE,
                List.of(new SeatResponse("1A", SeatStatus.AVAILABLE), new SeatResponse("1B", SeatStatus.BOOKED))));

        mockMvc.perform(get("/api/v1/flights/1/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seats[1].seatNumber").value("1B"))
                .andExpect(jsonPath("$.seats[1].status").value("BOOKED"));
    }

    @Test
    void seatMapReturns404ForUnknownFlight() throws Exception {
        when(flightService.getSeatMap(9L)).thenThrow(new NotFoundException(ErrorCode.FLIGHT_NOT_FOUND, "Flight 9 not found"));

        mockMvc.perform(get("/api/v1/flights/9/seats"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("FLIGHT_NOT_FOUND"));
    }
}
