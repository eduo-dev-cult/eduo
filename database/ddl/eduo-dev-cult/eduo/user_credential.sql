CREATE TABLE IF NOT EXISTS eduo.user_credential
(
    user_id       INTEGER    NOT NULL
        CONSTRAINT user_credential_pk
            PRIMARY KEY
        CONSTRAINT "FK_user_credential_user_id"
            REFERENCES eduo."user"
            ON UPDATE CASCADE ON DELETE CASCADE,
    username      VARCHAR(8) NOT NULL,
    password      VARCHAR    NOT NULL,
    last_login_at TIMESTAMP
);

ALTER TABLE eduo.user_credential
    OWNER TO postgres;

CREATE INDEX IF NOT EXISTS "AK"
    ON eduo.user_credential (username);

