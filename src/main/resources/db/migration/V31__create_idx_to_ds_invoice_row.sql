-- Add order_number index if not already exists
CREATE INDEX IF NOT EXISTS order_numberx ON public.dropshipment_invoice_row (order_number);

COMMENT ON INDEX order_numberx IS 'An index on order number';

-- Add invoice_number index if not already exists
CREATE INDEX IF NOT EXISTS invoice_numberx ON public.dropshipment_invoice_row (invoice_number);

COMMENT ON INDEX invoice_numberx IS 'An index on invoice number';
