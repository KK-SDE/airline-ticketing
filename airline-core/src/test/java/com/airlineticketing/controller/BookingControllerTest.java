package com.airlineticketing.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.airlineticketing.constants.BookingStatus;
import com.airlineticketing.constants.ErrorCode;
import com.airlineticketing.dto.BookingResponse;
import com.airlineticketing.dto.BookingSeatResponse;
import com.airlineticketing.exception.ConflictException;
import com.airlineticketing.exception.NotFoundException;
import com.airlineticketing.service.BookingService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    private static final UUID REFERENCE = UUID.fromString("1f58c428-6e7b-4ce4-9057-9d56bd7f5b4d");
    private static final String VALID_BODY = """
            {"flightId": 1, "passengers": [{"name": "Alice", "seatNumber": "1A"}]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @Test
    void createReturns201WithBody() throws Exception {
        when(bookingService.create(any())).thenReturn(response(BookingStatus.CONFIRMED));

        mockMvc.perform(post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingReference").value(REFERENCE.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.assignedSeats[0].seatNumber").value("1A"));
    }

    @Test
    void createReturnsStandardErrorForInvalidBody() throws Exception {
        String body = """
                {"flightId": null, "passengers": [{"name": "", "seatNumber": "A1"}]}
                """;

        mockMvc.perform(post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/v1/bookings"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors.length()").value(3));
    }

    @Test
    void createReturns400ForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));
    }

    @Test
    void createMapsSeatConflictTo409() throws Exception {
        when(bookingService.create(any()))
                .thenThrow(new ConflictException(ErrorCode.SEAT_ALREADY_BOOKED, "Seats already booked: [1A]"));

        mockMvc.perform(post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SEAT_ALREADY_BOOKED"))
                .andExpect(jsonPath("$.message").value("Seats already booked: [1A]"));
    }

    @Test
    void cancelReturnsCancelledBooking() throws Exception {
        when(bookingService.cancel(REFERENCE)).thenReturn(response(BookingStatus.CANCELLED));

        mockMvc.perform(post("/api/v1/bookings/{ref}/cancel", REFERENCE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void getMapsUnknownBookingTo404() throws Exception {
        when(bookingService.get(REFERENCE)).thenThrow(new NotFoundException(ErrorCode.BOOKING_NOT_FOUND, "not found"));

        mockMvc.perform(get("/api/v1/bookings/{ref}", REFERENCE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("BOOKING_NOT_FOUND"));
    }

    @Test
    void getReturns400ForMalformedReference() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    private static BookingResponse response(BookingStatus status) {
        return new BookingResponse(REFERENCE, "XY101", LocalDate.of(2026, 10, 5), 1,
                List.of(new BookingSeatResponse("1A", "Alice")), status);
    }
}
