CREATE TABLE payment (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL,
    CONSTRAINT uk_idempotency_key UNIQUE (idempotency_key),

    amount NUMERIC(15,2) NOT NULL CHECK (amount > 0),
    currency CHAR(3) NOT NULL,
    client_snapshot JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    finalized_at TIMESTAMPTZ,
    lease_expires_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL,
    CONSTRAINT chk_payment_status
    CHECK(
        status IN (
            'RECEIVED',
            'PROCESSING',
            'TO_ANALYZE',
            'APPROVED',
            'RECUSED',
            'FAIL',
            'CANCEL_ADMIN'
        )
    ),

    CONSTRAINT chk_finalized_status
    CHECK (
        (
            finalized_at IS NULL AND status IN ('RECEIVED', 'PROCESSING', 'TO_ANALYZE')
        )
        OR
        (
            finalized_at IS NOT NULL AND status IN ('APPROVED', 'RECUSED', 'FAIL', 'CANCEL_ADMIN')
        )
    )
);