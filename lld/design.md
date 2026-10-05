# Low-Level Design

## 1. Modules

```
airlineTicketing (parent pom, Java 21, Spring Boot 4.1.1)
├── airline-db     Liquibase changelogs only (jar on the core classpath)
└── airline-core   Spring Boot application
```

`airline-core` package layout (`com.airlineticketing`):

| Package | Contents |
|---|---|
| `controller` | `AdminFlightScheduleController`, `FlightController`, `BookingController` |
| `service`, `service.impl` | `FlightScheduleService`, `FlightInstanceService`, `FlightService`, `BookingService` and implementations |
| `repository` | One Spring Data repository per entity |
| `entity` | `BaseEntity` (audit fields, id-based equality), `Airport`, `Aircraft`, `FlightSchedule`, `FlightInstance`, `Booking`, `BookingSeat` |
| `dto` | Request/response records, `PageResponse`, `ErrorResponse` |
| `mapper` | `FlightScheduleMapper`, `FlightInstanceMapper`, `FlightMapper`, `BookingMapper` |
| `config` | `AirlineProperties`, `ClockConfig`, `JpaConfig`, `SchedulingConfig`, `TraceIdFilter` |
| `scheduler` | `FlightInstanceScheduler` |
| `exception` | `ApiException` hierarchy, `GlobalExceptionHandler` |
| `util` | `SeatLabelUtils`, `FlightTimeUtils` |
| `constants` | `ErrorCode`, `BookingStatus`, `SeatStatus` |

## 2. Classes and responsibilities

* **Entities** use `@Getter/@Setter` (never `@Data`), lazy associations, `@Enumerated(STRING)`, audit columns, and a `@Version` on `Booking`. Equality is by id.
* **`FlightScheduleServiceImpl`** - validates and creates schedules, then calls `FlightInstanceService.generateInstances`.
* **`FlightInstanceServiceImpl`** - `generateInstances(schedule)` inserts the missing operating dates in `[today, today + window]`; `extendBookingWindow()` does this for every schedule in pages of 100.
* **`FlightServiceImpl`** - flight search (window check, fixed ordering by departure, one grouped query for booked counts) and seat map.
* **`BookingServiceImpl`** - create, get and cancel bookings.
* **`SeatLabelUtils`** - seat label parsing/validation against `(rows, letters)`, label generation and row-then-letter ordering.
* **`FlightTimeUtils`** - UTC departure/arrival instants for a flight date; arrival time not after departure time means next day.
* **`Clock`** bean is injected into services so time-dependent rules are testable.

## 3. APIs

Base path `/api/v1`. Errors always use the contract in section 5.

| Method and path | Purpose | Success |
|---|---|---|
| `POST /admin/flight-schedules` | Create a schedule and generate its flight instances | `201` |
| `GET /admin/flight-schedules/{id}` | Read one schedule | `200` |
| `GET /admin/flight-schedules?page&size` | List schedules (paged) | `200` |
| `GET /flights/search?source&destination&date&page&size` | Search flights on a date | `200` (paged) |
| `GET /flights/{flightId}/seats` | Live seat map | `200` |
| `POST /bookings` | Create a booking | `201` |
| `GET /bookings/{bookingReference}` | Read a booking | `200` |
| `POST /bookings/{bookingReference}/cancel` | Cancel a booking | `200` |

There is deliberately no update or delete for schedules (schedules are immutable after creation per the assumptions).

**Create schedule request**

```json
{ "flightNumber": "XY101", "sourceAirportCode": "DXB", "destinationAirportCode": "LHR",
  "departureTime": "09:30", "arrivalTime": "13:45", "aircraftId": 1,
  "daysOfOperation": ["MONDAY", "WEDNESDAY", "FRIDAY"] }
```

**Create booking request / response**

```json
{ "flightId": 1, "passengers": [ { "name": "Alice", "seatNumber": "1A" }, { "name": "Bob", "seatNumber": "1B" } ] }
```
```json
{ "bookingReference": "1f58c428-6e7b-4ce4-9057-9d56bd7f5b4d", "flightNumber": "XY101", "flightDate": "2026-10-05",
  "passengerCount": 2, "assignedSeats": [ { "seatNumber": "1A", "passengerName": "Alice" }, { "seatNumber": "1B", "passengerName": "Bob" } ],
  "status": "CONFIRMED" }
```

**Search response item**: `flightId, flightNumber, sourceAirportCode, destinationAirportCode, flightDate, departureTime, arrivalTime, availableSeats`.

## 4. Validation

| Where | Rule |
|---|---|
| Request DTOs (Bean Validation) | flight number `^[A-Z0-9]{2}[0-9]{1,4}$`; IATA codes 3 letters; times, aircraft, non-empty days; passenger name non-blank, max 100; seat number `^[0-9]{1,3}[A-Za-z]$`; at least one passenger |
| Search parameters | `source` and `destination` are 3 letters; `date` is ISO `yyyy-MM-dd` |
| Services (business) | source differs from destination; flight number unique; airports and aircraft exist; travel date within `[today, today+365]`; seats count at most 9; no duplicate seats; flight not departed; seats exist in the aircraft layout; seats not already booked; booking not already cancelled |

## 5. Error handling

`GlobalExceptionHandler` returns:

```json
{ "timestamp": "...", "status": 409, "errorCode": "SEAT_ALREADY_BOOKED", "message": "...",
  "path": "/api/v1/bookings", "traceId": "...", "fieldErrors": [ { "field": "date", "message": "..." } ] }
```

| Exception | HTTP | Codes |
|---|---|---|
| `BadRequestException` | 400 | `SAME_SOURCE_AND_DESTINATION`, `TRAVEL_DATE_OUT_OF_WINDOW`, `TOO_MANY_SEATS`, `DUPLICATE_SEAT_SELECTION`, `INVALID_SEAT` |
| `NotFoundException` | 404 | `AIRPORT_NOT_FOUND`, `AIRCRAFT_NOT_FOUND`, `SCHEDULE_NOT_FOUND`, `FLIGHT_NOT_FOUND`, `BOOKING_NOT_FOUND` |
| `ConflictException` | 409 | `FLIGHT_NUMBER_ALREADY_EXISTS`, `SEAT_ALREADY_BOOKED`, `FLIGHT_ALREADY_DEPARTED`, `BOOKING_ALREADY_CANCELLED` |
| Bean Validation, bad parameters, malformed JSON | 400 | `VALIDATION_FAILED` (with `fieldErrors`), `MALFORMED_REQUEST` |
| Lock/optimistic-lock failures | 409 | `CONCURRENT_UPDATE` |
| Unexpected `DataIntegrityViolationException` | 409 | `DATA_CONFLICT` |
| Anything else | 500 | `INTERNAL_ERROR` (generic message, details only in logs) |

## 6. Transaction boundaries

| Operation | Boundary |
|---|---|
| `FlightScheduleService.create` | one transaction: schedule, operating days and all generated instances |
| `FlightInstanceService.extendBookingWindow` | one transaction for the whole run (inserts at most one new date per schedule per day) |
| `BookingService.create` | one transaction: flight row lock, validation, booking and seat inserts, flush |
| `BookingService.cancel` | one transaction: booking row lock, status change, seat release |
| Read operations | `@Transactional(readOnly = true)`; DTOs are produced inside the transaction |

## 7. Key algorithms

**Search.** Query `flight_instance` where `flight_date = :date`, route matches the schedule's source and destination, and `departure_at > now`, ordered by `departure_at, id`. Capacity = `seat_rows * length(seat_letters)`; booked counts for the page come from one `GROUP BY flight_instance_id` query on active seats; `availableSeats = capacity - booked`.

**Booking.** Normalize and validate the seat list, lock the flight row, check departure and layout, check active seats, insert booking and seats, flush, return. A unique-index violation at flush is mapped to `SEAT_ALREADY_BOOKED`.

**Cancellation.** Lock the booking row, reject if already cancelled, set `CANCELLED` and `cancelled_at`, set `active = false` on its seats, return the booking.

**Window maintenance.** `FlightInstanceScheduler` runs at 00:05 UTC (`airline.flight-generation.cron`) and on startup, with a fresh trace id per run and a ShedLock lock named `flightInstanceGeneration`.

## 8. Observability

Every log line carries `traceId` (from `X-Trace-Id` when it is a safe value, otherwise generated). Actuator exposes `health` (liveness/readiness probes), `metrics` and `prometheus`.

## 9. Testing approach

| Level | Tests |
|---|---|
| Unit (JUnit 5 + Mockito) | `BookingServiceImplTest`, `FlightScheduleServiceImplTest`, `FlightServiceImplTest`, `FlightInstanceServiceImplTest`, `SeatLabelUtilsTest`, `FlightTimeUtilsTest` |
| Controller (`@WebMvcTest`) | status codes, validation errors and the standard error shape |
| Integration (Testcontainers + real PostgreSQL, run by `mvn verify`) | Liquibase migrations plus seed data; 20 threads racing for one seat (exactly one wins); 20 threads on distinct seats (all win); overlapping multi-seat bookings (exactly one wins); concurrent cancellations (exactly one wins, seats released and re-bookable) |
