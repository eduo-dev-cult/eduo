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

