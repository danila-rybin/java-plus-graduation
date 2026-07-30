-- Таблица пользователей
CREATE TABLE users
(
    id    BIGSERIAL PRIMARY KEY,
    name  TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE
);

-- Таблица категорий
CREATE TABLE categories
(
    id   BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

-- Таблица событий
CREATE TABLE events
(
    id                 BIGSERIAL PRIMARY KEY,
    title              TEXT,
    annotation         TEXT,
    description        TEXT,
    event_date         TIMESTAMP,
    initiator_id       BIGINT REFERENCES users (id),
    category_id        BIGINT REFERENCES categories (id),
    paid               BOOLEAN,
    participant_limit  INT,
    request_moderation BOOLEAN,
    status             TEXT CHECK (status IN ('PENDING', 'PUBLISHED', 'CANCELED')),
    created_on         TIMESTAMP,
    published_on       TIMESTAMP,
    confirmed_requests BIGINT DEFAULT 0
);

-- Таблица подборок (compilations)
CREATE TABLE compilations
(
    id     BIGSERIAL PRIMARY KEY,
    title  TEXT NOT NULL,
    pinned BOOLEAN DEFAULT FALSE
);

-- Связующая таблица для compilation -> events
CREATE TABLE compilation_events
(
    compilation_id BIGINT REFERENCES compilations (id) ON DELETE CASCADE,
    event_id       BIGINT REFERENCES events (id) ON DELETE CASCADE,
    PRIMARY KEY (compilation_id, event_id)
);