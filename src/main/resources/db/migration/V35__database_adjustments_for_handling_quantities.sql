ALTER TABLE "public".dropshipment_invoice_row ADD COLUMN "quantity" integer NOT NULL;

COMMENT ON COLUMN public.dropshipment_invoice_row.quantity
    IS 'The quantity for invoiced order row';

ALTER TABLE public.sales_order ADD COLUMN shipped boolean NOT NULL DEFAULT false;

COMMENT ON COLUMN public.sales_order.shipped
    IS 'The flag for fully shipped orders';