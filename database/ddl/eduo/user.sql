CREATE TABLE IF NOT EXISTS eduo."user"
(
    user_id    INTEGER GENERATED ALWAYS AS IDENTITY,
    first_name VARCHAR NOT NULL,
    last_name  VARCHAR NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

ALTER TABLE eduo."user"
    ADD PRIMARY KEY (user_id);

