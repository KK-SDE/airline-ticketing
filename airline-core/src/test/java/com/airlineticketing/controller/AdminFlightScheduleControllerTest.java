package com.airlineticketing.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.airlineticketing.dto.FlightScheduleResponse;
import com.airlineticketing.service.FlightScheduleService;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminFlightScheduleController.class)
class AdminFlightScheduleControllerTest {

    private static final String VALID_BODY = """
            {"flightNumber": "XY101", "sourceAirportCode": "DXB", "destinationAirportCode": "LHR",
             "departureTime": "09:30", "arrivalTime": "13:45", "aircraftId": 1,
             "daysOfOperation": ["MONDAY", "WEDNESDAY", "FRIDAY"]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FlightScheduleService flightScheduleService;

    @Test
    void createReturns201WithBody() throws Exception {
        when(flightScheduleService.create(any())).thenReturn(response());

        mockMvc.perform(post("/api/v1/admin/flight-schedules").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flightNumber").value("XY101"))
                .andExpect(jsonPath("$.daysOfOperation.length()").value(3));
    }

    @Test
    void createValidatesFlightNumberDaysAndRequiredFields() throws Exception {
        String body = """
                {"flightNumber": "bad", "sourceAirportCode": "DXB", "destinationAirportCode": "LHR",
                 "daysOfOperation": []}
                """;

        mockMvc.perform(post("/api/v1/admin/flight-schedules").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(5));
    }

    @Test
    void createRejectsUnknownDayName() throws Exception {
        String body = VALID_BODY.replace("FRIDAY", "FUNDAY");

        mockMvc.perform(post("/api/v1/admin/flight-schedules").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));
    }

    @Test
    void listReturnsPageResponse() throws Exception {
        when(flightScheduleService.list(any())).thenReturn(new PageImpl<>(List.of(response())));

        mockMvc.perform(get("/api/v1/admin/flight-schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(7))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    private static FlightScheduleResponse response() {
        return new FlightScheduleResponse(7L, "XY101", "DXB", "LHR", LocalTime.of(9, 30), LocalTime.of(13, 45), 1L,
                "A320", Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), Instant.parse("2026-10-04T09:00:00Z"));
    }
}
