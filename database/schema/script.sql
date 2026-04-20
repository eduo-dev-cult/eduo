-- creates the database schema in its entirety
CREATE SCHEMA IF NOT EXISTS eduo;

SET search_path TO eduo;

-- keep the above when editing the below

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

CREATE TABLE IF NOT EXISTS eduo.user_preferences
(
    user_id INTEGER NOT NULL,
    locale  VARCHAR
);

ALTER TABLE eduo.user_preferences
    ADD CONSTRAINT "FK_user_preferences_user_id"
        FOREIGN KEY (user_id) REFERENCES eduo."user"
            ON UPDATE CASCADE ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS eduo.user_credential
(
    user_id       INTEGER,
    username      VARCHAR(8) NOT NULL,
    password      VARCHAR    NOT NULL,
    last_login_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS "AK"
    ON eduo.user_credential (username);

ALTER TABLE eduo.user_credential
    ADD CONSTRAINT "FK_user_credential_user_id"
        FOREIGN KEY (user_id) REFERENCES eduo."user"
            ON UPDATE CASCADE ON DELETE CASCADE;


