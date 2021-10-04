ALTER TABLE "public".sales_order_invoice DROP CONSTRAINT fk_sasles_order_invoice_sales_order;
ALTER TABLE "public".sales_order_invoice ALTER COLUMN sales_order_id DROP NOT NULL;
ALTER TABLE "public".sales_order_invoice ADD COLUMN order_number varchar(100) NOT NULL DEFAULT '';
ALTER TABLE "public".sales_order_invoice ALTER COLUMN order_number DROP DEFAULT;
