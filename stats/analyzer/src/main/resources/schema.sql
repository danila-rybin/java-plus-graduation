CREATE TABLE IF NOT EXISTS user_actions
(
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    event_id    BIGINT      NOT NULL,
    action_type VARCHAR(20) NOT NULL,
    weight      DOUBLE PRECISION NOT NULL,
    timestamp   TIMESTAMP   NOT NULL
);

CREATE INDEX idx_user_actions_user_id ON user_actions (user_id);
CREATE INDEX idx_user_actions_event_id ON user_actions (event_id);
CREATE INDEX idx_user_actions_timestamp ON user_actions (timestamp);

CREATE TABLE IF NOT EXISTS event_similarities
(
    id        BIGSERIAL PRIMARY KEY,
    event_a   BIGINT           NOT NULL,
    event_b   BIGINT           NOT NULL,
    score     DOUBLE PRECISION NOT NULL,
    timestamp TIMESTAMP        NOT NULL,
    CONSTRAINT unique_event_pair UNIQUE (event_a, event_b)
);

CREATE INDEX idx_event_similarities_event_a ON event_similarities (event_a);
CREATE INDEX idx_event_similarities_event_b ON event_similarities (event_b);
CREATE INDEX idx_event_similarities_score ON event_similarities (score);