ALTER TABLE "public".sales_order_invoice ADD COLUMN "source" varchar(20);

CREATE INDEX IF NOT EXISTS source_idx ON "public".sales_order_invoice ("source");

COMMENT ON COLUMN "public".sales_order_invoice."source" IS 'what kind of resource that invoice is generated from';