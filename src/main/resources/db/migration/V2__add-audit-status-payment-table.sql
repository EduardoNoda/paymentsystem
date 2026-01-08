CREATE TABLE payment_audit (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    CONSTRAINT fk_payment_id FOREIGN KEY (payment_id) REFERENCES payment(id),
    status VARCHAR(32) NOT NULL,
    action_origin VARCHAR(32) NOT NULL,
    description TEXT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT chk_payment_audit_status
    CHECK (
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

    CONSTRAINT chk_action_origin
    CHECK (
        action_origin IN ('API', 'JOB', 'ADMIN')
    )

    CREATE INDEX idx_payment_audit_payment_id
    ON payment_audit(payment_id, occurred_at DESC);
);