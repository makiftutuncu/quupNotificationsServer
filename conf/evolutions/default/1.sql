# --- !Ups
CREATE TABLE Registration (
    registrationId     VARCHAR(512) PRIMARY KEY,
    sessionId          VARCHAR(256) NOT NULL,
    lastNotification   BIGINT UNSIGNED NOT NULL
);

# --- !Downs