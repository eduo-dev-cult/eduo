CREATE TABLE IF NOT EXISTS eduo.user_preferences
(
    user_id INTEGER NOT NULL
        CONSTRAINT user_preferences_pk
            PRIMARY KEY
        CONSTRAINT "FK_user_preferences_user_id"
            REFERENCES eduo."user"
            ON UPDATE CASCADE ON DELETE CASCADE,
    locale  VARCHAR
);

ALTER TABLE eduo.user_preferences
    OWNER TO postgres;

