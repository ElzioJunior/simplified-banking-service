CREATE TABLE transfer_idempotency_tokens (
    token UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    operation_id UUID,
    source_account_id BIGINT,
    destination_account_id BIGINT,
    amount NUMERIC(19, 2),
    CONSTRAINT pk_transfer_idempotency_tokens PRIMARY KEY (token),
    CONSTRAINT uq_transfer_idempotency_tokens_operation UNIQUE (operation_id),
    CONSTRAINT fk_transfer_tokens_source_account
        FOREIGN KEY (source_account_id) REFERENCES accounts (id) ON DELETE RESTRICT,
    CONSTRAINT fk_transfer_tokens_destination_account
        FOREIGN KEY (destination_account_id) REFERENCES accounts (id) ON DELETE RESTRICT,
    CONSTRAINT chk_transfer_tokens_expiration CHECK (expires_at > created_at),
    CONSTRAINT chk_transfer_tokens_association CHECK (
        (used_at IS NULL
            AND operation_id IS NULL
            AND source_account_id IS NULL
            AND destination_account_id IS NULL
            AND amount IS NULL)
        OR
        (used_at IS NOT NULL
            AND operation_id IS NOT NULL
            AND source_account_id IS NOT NULL
            AND destination_account_id IS NOT NULL
            AND amount IS NOT NULL)
    ),
    CONSTRAINT chk_transfer_tokens_distinct_accounts CHECK (
        source_account_id IS NULL OR source_account_id <> destination_account_id
    ),
    CONSTRAINT chk_transfer_tokens_amount_positive CHECK (amount IS NULL OR amount > 0)
);

CREATE INDEX idx_transfer_tokens_unused_expiration
    ON transfer_idempotency_tokens (expires_at)
    WHERE used_at IS NULL;

CREATE TABLE transfer_notification_outbox (
    event_id UUID NOT NULL,
    operation_id UUID NOT NULL,
    recipient_account_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    publish_attempts INTEGER NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMPTZ,
    CONSTRAINT pk_transfer_notification_outbox PRIMARY KEY (event_id),
    CONSTRAINT fk_transfer_outbox_recipient_account
        FOREIGN KEY (recipient_account_id) REFERENCES accounts (id) ON DELETE RESTRICT,
    CONSTRAINT uq_transfer_outbox_operation_recipient_event
        UNIQUE (operation_id, recipient_account_id, event_type),
    CONSTRAINT chk_transfer_outbox_event_type CHECK (event_type = 'TRANSFER_COMPLETED'),
    CONSTRAINT chk_transfer_outbox_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_transfer_outbox_publish_attempts CHECK (publish_attempts >= 0)
);

CREATE INDEX idx_transfer_outbox_pending
    ON transfer_notification_outbox (occurred_at, event_id)
    WHERE published_at IS NULL;
