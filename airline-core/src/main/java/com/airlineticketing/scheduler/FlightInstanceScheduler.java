package com.airlineticketing.scheduler;

import com.airlineticketing.config.TraceIdFilter;
import com.airlineticketing.service.FlightInstanceService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.MDC;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlightInstanceScheduler {

    private final FlightInstanceService flightInstanceService;

    @Scheduled(cron = "${airline.flight-generation.cron}", zone = "UTC")
    @SchedulerLock(name = "flightInstanceGeneration", lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
    public void extendBookingWindow() {
        run();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void extendBookingWindowOnStartup() {
        run();
    }

    private void run() {
        MDC.put(TraceIdFilter.TRACE_ID_KEY, UUID.randomUUID().toString().replace("-", ""));
        try {
            log.info("Flight instance generation started");
            int created = flightInstanceService.extendBookingWindow();
            log.info("Flight instance generation finished, created={}", created);
        } catch (RuntimeException e) {
            log.error("Flight instance generation failed", e);
        } finally {
            MDC.remove(TraceIdFilter.TRACE_ID_KEY);
        }
    }
}
