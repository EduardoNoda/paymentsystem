CREATE OR REPLACE FUNCTION fn_audit_payment_status_change()
RETURNS trigger AS $$
BEGIN
    IF NEW.status IS DISTINCT FROM OLD.status THEN
        INSERT INTO payment_audit (
            payment_id,
            status,
            action_origin,
            description,
            occurred_at
        )
        VALUES (
            NEW.id,
            NEW.status,
            current_setting('app.action_origin', true),
            format('Status changed from %s -> %s', OLD.status, NEW.status),
            now()
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tgr_payment_audit_status
AFTER UPDATE OF status ON payment
FOR EACH ROW
EXECUTE FUNCTION fn_audit_payment_status_change();