package com.airlineticketing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.airlineticketing.repository.AircraftRepository;
import com.airlineticketing.repository.AirportRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AirlineTicketingApplicationIT extends PostgresIntegrationTest {

    @Autowired
    private AirportRepository airportRepository;
    @Autowired
    private AircraftRepository aircraftRepository;

    @Test
    void liquibaseMigrationsApplyAndSeedReferenceData() {
        assertThat(airportRepository.existsById("DXB")).isTrue();
        assertThat(airportRepository.count()).isEqualTo(8);
        assertThat(aircraftRepository.count()).isEqualTo(3);
    }
}
