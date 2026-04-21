SET search_path TO eduo;

WITH new_users AS (
    INSERT INTO "user" (first_name, last_name)
    VALUES
        ('Alice', 'Svensson'),
        ('Bob',   'Lindqvist'),
        ('Carol', 'Eriksson')
    RETURNING user_id, first_name
), -- use user and firstname from previous table to
new_prefs AS (
    INSERT INTO user_preferences (user_id, locale)
    SELECT u.user_id, p.locale
    FROM new_users u
    JOIN (VALUES ('Alice', 'sv-SE'), ('Bob', 'en-GB')) AS p(name, locale)
      ON u.first_name = p.name
)
INSERT INTO user_credential (user_id, username, password)
SELECT u.user_id, c.username, c.password
FROM new_users u
JOIN (VALUES
    ('Alice', 'alisve-5', 'placeholder'),
    ('Bob',   'boblin-3', 'placeholder'),
    ('Carol', 'careri-5', 'placeholder')
) AS c(name, username, password) ON u.first_name = c.name;
