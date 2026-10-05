--liquibase formatted sql

--changeset airline:202610041504-seed-airports
INSERT INTO airport (code, name, city, country) VALUES
    ('DXB', 'Dubai International Airport', 'Dubai', 'United Arab Emirates'),
    ('LHR', 'Heathrow Airport', 'London', 'United Kingdom'),
    ('JFK', 'John F. Kennedy International Airport', 'New York', 'United States'),
    ('DEL', 'Indira Gandhi International Airport', 'Delhi', 'India'),
    ('BOM', 'Chhatrapati Shivaji Maharaj International Airport', 'Mumbai', 'India'),
    ('SIN', 'Singapore Changi Airport', 'Singapore', 'Singapore'),
    ('CDG', 'Charles de Gaulle Airport', 'Paris', 'France'),
    ('SYD', 'Sydney Kingsford Smith Airport', 'Sydney', 'Australia');
--rollback DELETE FROM airport WHERE code IN ('DXB', 'LHR', 'JFK', 'DEL', 'BOM', 'SIN', 'CDG', 'SYD');

--changeset airline:202610041504-seed-aircraft
INSERT INTO aircraft (aircraft_type, seat_rows, seat_letters) VALUES
    ('A320', 30, 'ABCDEF'),
    ('B737-800', 32, 'ABCDEF'),
    ('A220-300', 25, 'ABCD');
--rollback DELETE FROM aircraft WHERE aircraft_type IN ('A320', 'B737-800', 'A220-300');
