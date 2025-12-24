CREATE OR REPLACE FUNCTION validate_payment_status_transition()
RETURNS trigger AS $$
BEGIN
    IF OLD.status = NEW.status THEN
        RETURN OLD;
    END IF;

    IF NOT (
        (OLD.status = 'RECEIVED' AND NEW.status = 'PROCESSING') OR
        (OLD.status = 'PROCESSING' AND NEW.status IN ('APPROVED', 'RECUSED', 'FAIL', 'TO_ANALYZE')) OR
        (OLD.status = 'TO_ANALYZE' AND NEW.status IN ('PROCESSING', 'CANCEL_ADMIN'))
    ) THEN
        RAISE EXCEPTION
            'Invalid payment status transition % -> % for payment %', OLD.status, NEW.status, OLD.id;
    END IF;

    RETURN NEW;
END;

$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_payment_status_transition
BEFORE UPDATE OF status ON payment
FOR EACH ROW
EXECUTE FUNCTION validate_payment_status_transition();

CREATE OR REPLACE FUNCTION current_action_origin()
RETURNS TEXT AS $$
BEGIN
    RETURN current_setting('app.action_origin', true);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION enforce_action_authority()
RETURNS trigger AS $$
DECLARE
    origin TEXT;
BEGIN
    IF OLD.status = NEW.status THEN
        RETURN OLD;
    END IF;

    origin := current_action_origin();

    IF origin IS NULL THEN
        RAISE EXCEPTION 'Action origin not set';
    END IF;

    IF origin = 'API' THEN
        IF NOT (
           OLD.status = 'RECEIVED' AND NEW.status = 'PROCESSING' OR
           OLD.status = 'PROCESSING' AND NEW.status IN ('APPROVED', 'RECUSED', 'FAIL')
        ) THEN
            RAISE EXCEPTION
                'API not allowed to perform transition % -> %',
                OLD.status, NEW.status;
           END IF;

        ELSIF origin = 'JOB' THEN
           IF NOT (
                OLD.status = 'PROCESSING' AND NEW.status = 'TO_ANALYZE' OR
                OLD.status = 'TO_ANALYZE' AND NEW.status = 'PROCESSING'
           ) THEN
                RAISE EXCEPTION
                    'JOB not allowed to perform transition % -> %',
                    OLD.status, NEW.status;
           END IF;

       ELSIF origin = 'ADMIN' THEN
           IF OLD.status <> 'TO_ANALYZE' THEN
               RAISE EXCEPTION
                   'ADMIN not allowed to perform transition % -> %',
                   OLD.status, NEW.status;
           END IF;

       ELSE
           RAISE EXCEPTION 'Unknown action origin: %', origin;
       END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_enforce_action_authority
BEFORE UPDATE OF status ON payment
FOR EACH ROW
EXECUTE FUNCTION enforce_action_authority();

CREATE OR REPLACE FUNCTION enforce_lease_control()
RETURNS trigger AS $$
DECLARE
    origin TEXT;
BEGIN
    IF OLD.status = NEW.status THEN
        RETURN OLD;
    END IF;

    origin := current_action_origin();

    IF origin <> 'ADMIN'
        AND OLD.status IN ('PROCESSING', 'TO_ANALYZE')
        AND OLD.lease_expires_at IS NOT NULL
        AND OLD.lease_expires_at > now()
    THEN
        RAISE EXCEPTION
            'Payment % is locked until %',
            OLD.id, OLD.lease_expires_at;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_enforce_lease_control
BEFORE UPDATE OF status ON payment
FOR EACH ROW
EXECUTE FUNCTION enforce_lease_control();

CREATE OR REPLACE FUNCTION update_payment_timestamps()
RETURNS trigger AS $$
BEGIN
    IF OLD.status = NEW.status THEN
        RETURN OLD;
    END IF;

    NEW.updated_at := now();

    IF NEW.status IN ('APPROVED', 'RECUSED', 'FAIL', 'CANCEL_ADMIN') THEN
        NEW.finalized_at := now();
        NEW.lease_expires_at := NULL;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_payment_timestamps
BEFORE UPDATE ON payment
FOR EACH ROW
EXECUTE FUNCTION update_payment_timestamps();

CREATE OR REPLACE FUNCTION audit_payment_status_change()
RETURNS trigger AS $$
BEGIN
    IF OLD.status = NEW.status THEN
        RETURN NEW;
    END IF;

    INSERT INTO payment_audit (
        payment_id,
        status,
        action_origin,
        description,
        occurred_at
    ) VALUES (
        NEW.id,
        NEW.status,
        current_action_origin(),
        format('Status changed from % to %', OLD.status, NEW.status),
        now()
    );

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_payment_status_change
AFTER UPDATE OF status ON payment
FOR EACH ROW
EXECUTE FUNCTION audit_payment_status_change();