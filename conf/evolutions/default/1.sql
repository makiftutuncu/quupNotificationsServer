# --- !Ups
CREATE TABLE Data (
    registrationId     varchar(256) PRIMARY KEY,
    trackingId         varchar(128) NOT NULL,
    sessionId          varchar(256) NOT NULL,
    lastNotificationId varchar(128),
    lastMentionId      varchar(128),
    lastMessageId      varchar(128)
)

# --- !Downs
