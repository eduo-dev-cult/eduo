CREATE TABLE IF NOT EXISTS eduo."user"
(
    user_id    INTEGER GENERATED ALWAYS AS IDENTITY
        PRIMARY KEY,
    first_name VARCHAR NOT NULL,
    last_name  VARCHAR NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

ALTER TABLE eduo."user"
    OWNER TO postgres;

