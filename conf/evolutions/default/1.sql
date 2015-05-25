# --- !Ups
CREATE TABLE Data (
    registrationId     varchar(1024) PRIMARY KEY,
    trackingId         varchar(512) NOT NULL,
    sessionId          varchar(1024) NOT NULL,
    lastNotificationId varchar(512),
    lastMentionId      varchar(512),
    lastMessageId      varchar(512)
)

# --- !Downs
