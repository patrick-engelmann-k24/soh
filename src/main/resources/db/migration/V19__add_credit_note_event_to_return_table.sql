ALTER TABLE return_order ADD COLUMN credit_note_event json;

COMMENT ON COLUMN return_order.credit_note_event IS 'credit note event to be forwarder to ERP';
