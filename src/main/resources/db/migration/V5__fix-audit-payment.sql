-- Remove trigger antigo
DROP TRIGGER IF EXISTS tgr_payment_audit_status ON payment;

-- Remove função antiga
DROP FUNCTION IF EXISTS fn_audit_payment_status_change();

-- Função definitiva
CREATE OR REPLACE FUNCTION audit_payment_status_change()
RETURNS trigger AS $$
BEGIN
  IF NEW.status IS DISTINCT FROM OLD.status THEN
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
      format('Status changed from %s -> %s', OLD.status, NEW.status),
      now()
    );
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger definitivo
CREATE TRIGGER tgr_payment_audit_status
AFTER UPDATE OF status ON payment
FOR EACH ROW
EXECUTE FUNCTION audit_payment_status_change();
