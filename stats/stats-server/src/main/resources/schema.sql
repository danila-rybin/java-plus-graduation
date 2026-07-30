CREATE TABLE IF NOT EXISTS endpoint_hits (
    id BIGSERIAL PRIMARY KEY,
    app VARCHAR(255) NOT NULL,
    uri VARCHAR(1024) NOT NULL,
    ip VARCHAR(64) NOT NULL,
    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_endpoint_hits_timestamp
    ON endpoint_hits (timestamp);

CREATE INDEX IF NOT EXISTS idx_endpoint_hits_app_uri
    ON endpoint_hits (app, uri);

CREATE INDEX IF NOT EXISTS idx_endpoint_hits_ip
    ON endpoint_hits (ip);