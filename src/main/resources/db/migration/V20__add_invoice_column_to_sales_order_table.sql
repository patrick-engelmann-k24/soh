ALTER TABLE "public".sales_order ADD COLUMN "invoice_event" json;

COMMENT ON COLUMN "public".sales_order."invoice_event" IS 'core sales invoice created event to be forwarder to ERP';