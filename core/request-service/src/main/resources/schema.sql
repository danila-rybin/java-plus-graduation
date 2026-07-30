CREATE TABLE IF NOT EXISTS requests
(
    id           BIGSERIAL PRIMARY KEY,
    created      TIMESTAMP,
    event_id     BIGINT,
    requester_id BIGINT,
    status       VARCHAR(20) CHECK (status IN ('PENDING', 'CONFIRMED', 'REJECTED', 'CANCELED')),
    UNIQUE (event_id, requester_id)
);

CREATE INDEX IF NOT EXISTS idx_requests_event ON requests(event_id);
CREATE INDEX IF NOT EXISTS idx_requests_requester ON requests(requester_id);
CREATE INDEX IF NOT EXISTS idx_requests_status ON requests(status);