CREATE TABLE IF NOT EXISTS eduo.user_preferences
(
    user_id INTEGER NOT NULL,
    locale  VARCHAR
);

ALTER TABLE eduo.user_preferences
    ADD CONSTRAINT "FK_user_preferences_user_id"
        FOREIGN KEY (user_id) REFERENCES eduo."user"
            ON UPDATE CASCADE ON DELETE CASCADE;

