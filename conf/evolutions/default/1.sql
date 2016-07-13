# --- !Ups
CREATE TABLE Users (
    registration     VARCHAR(512) PRIMARY KEY,
    session          VARCHAR(256) NOT NULL,
    lastNotification INT UNSIGNED NOT NULL
);

# --- !Downs