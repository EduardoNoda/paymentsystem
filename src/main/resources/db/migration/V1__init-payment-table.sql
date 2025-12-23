    CREATE TYPE payment_status AS ENUM (
        RECEIVED,
        PROCESSING,
        TO_ANALYZE,
        APPROVED,
        RECUSED,
        FAIL,
        ADMIN_CANCEL
    );

    CREATE TABLE payment (
        id BIGSERIAL PRIMARY KEY NOT NULL,
        idempotency_key VARCHAR(128) NOT NULL,
        CONSTRAINT uk_idempotency_key UNIQUE (idempotency_key),

        amount NUMERIC(15,2) NOT NULL CHECK (amount > 0),
        currency CHAR(3) NOT NULL,
        client_snapshot JSONB NOT NULL,
        created_at TIMESTAMPTZ NOT NULL,

        updated_at TIMESTAMPTZ NOT NULL,
        finalized_at TIMESTAMPTZ,
        lease_expires_at TIMESTAMPTZ,
        status payment_status NOT NULL,
        CONSTRAINT chk_finalized_status
        CHECK
        (finalized_at IS NULL AND status IN ('RECEIVED', 'PROCESSING', 'TO_ANALYZE'))
        OR
        (finalized_at IS NOT NULL AND status IN ('APPROVED', 'RECUSED', 'FAIL', 'ADMIN_CANCEL'))
    );