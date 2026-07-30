CREATE TABLE IF NOT EXISTS categories
(
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS events
(
    id                 BIGSERIAL PRIMARY KEY,
    title              VARCHAR(120),
    annotation         VARCHAR(2000),
    description        VARCHAR(7000),
    event_date         TIMESTAMP,
    initiator_id       BIGINT,
    category_id        BIGINT REFERENCES categories (id),
    lat                DOUBLE PRECISION,
    lon                DOUBLE PRECISION,
    paid               BOOLEAN,
    participant_limit  INT,
    request_moderation BOOLEAN,
    status             VARCHAR(20) CHECK (status IN ('PENDING', 'PUBLISHED', 'CANCELED')),
    created_on         TIMESTAMP,
    published_on       TIMESTAMP,
    confirmed_requests BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_events_initiator ON events(initiator_id);
CREATE INDEX IF NOT EXISTS idx_events_category ON events(category_id);
CREATE INDEX IF NOT EXISTS idx_events_status ON events(status);
CREATE INDEX IF NOT EXISTS idx_events_event_date ON events(event_date);

CREATE TABLE IF NOT EXISTS compilations
(
    id     BIGSERIAL PRIMARY KEY,
    title  VARCHAR(50) NOT NULL UNIQUE,
    pinned BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS compilation_events
(
    compilation_id BIGINT REFERENCES compilations (id) ON DELETE CASCADE,
    event_id       BIGINT REFERENCES events (id) ON DELETE CASCADE,
    PRIMARY KEY (compilation_id, event_id)
);