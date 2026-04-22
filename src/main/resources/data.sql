-- should add test data on startup, or not if it's already inserted
DO $$
BEGIN
    -- check if the first test user exists, abort the whole thing if found (implies non-fresh db)
    IF NOT EXISTS (SELECT 1 FROM eduo."user" WHERE first_name = 'Alice' AND last_name = 'Svensson') THEN
    WITH new_users AS (
        INSERT INTO eduo."user" (first_name, last_name)
            VALUES
                ('Alice', 'Svensson'),
                ('Bob',   'Lindqvist'),
                ('Carol', 'Eriksson')
            ON CONFLICT DO NOTHING
            RETURNING user_id, first_name
    ), -- use user and firstname from previous table to
         new_prefs AS (
             INSERT INTO eduo.user_preferences (user_id, locale)
                 SELECT u.user_id, p.locale
                 FROM new_users u
                          JOIN (VALUES ('Alice', 'sv-SE'), ('Bob', 'en-GB')) AS p(name, locale)
                               ON u.first_name = p.name
         )
    INSERT INTO eduo.user_credential (user_id, username, password)
    SELECT u.user_id, c.username, c.password
    FROM new_users u
             JOIN (VALUES
                       ('Alice', 'alisve-5', 'placeholder'),
                       ('Bob',   'boblin-3', 'placeholder'),
                       ('Carol', 'careri-5', 'placeholder')
    ) AS c(name, username, password) ON u.first_name = c.name;
    END IF;
END $$;;