-- Base propriétaire : notification_db (aucune table partagée avec les autres services)
CREATE TABLE notifications (
    id              UUID            PRIMARY KEY,
    version         BIGINT          NOT NULL DEFAULT 0,
    event_id        UUID            NOT NULL,
    event_type      VARCHAR(100)    NOT NULL,
    aggregate_id    VARCHAR(100),
    correlation_id  VARCHAR(100),
    recipient       VARCHAR(320)    NOT NULL,
    channel         VARCHAR(20)     NOT NULL,
    subject         VARCHAR(255)    NOT NULL,
    message         TEXT            NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    attempt_count   INTEGER         NOT NULL DEFAULT 0,
    provider        VARCHAR(30),
    created_at      TIMESTAMPTZ     NOT NULL,
    sent_at         TIMESTAMPTZ,
    next_attempt_at TIMESTAMPTZ,
    last_error      VARCHAR(1000),

    -- Idempotence : un même événement ne produit qu'une notification par canal et destinataire
    CONSTRAINT uq_notification_event_channel_recipient UNIQUE (event_id, channel, recipient),
    CONSTRAINT ck_notification_status CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'SKIPPED')),
    CONSTRAINT ck_notification_channel CHECK (channel IN ('EMAIL')),
    CONSTRAINT ck_notification_attempts CHECK (attempt_count >= 0)
);

CREATE INDEX idx_notifications_due ON notifications (next_attempt_at) WHERE status = 'PENDING';
CREATE INDEX idx_notifications_recipient ON notifications (recipient, created_at DESC);
CREATE INDEX idx_notifications_status ON notifications (status, created_at DESC);
CREATE INDEX idx_notifications_correlation ON notifications (correlation_id);
