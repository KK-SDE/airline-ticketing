package com.airlineticketing.service;

import com.airlineticketing.entity.FlightSchedule;

public interface FlightInstanceService {

    int generateInstances(FlightSchedule schedule);

    int extendBookingWindow();
}
