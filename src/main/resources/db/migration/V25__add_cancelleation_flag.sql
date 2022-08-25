ALTER TABLE public.sales_order
    ADD COLUMN cancelled boolean DEFAULT false NOT NULL;

COMMENT ON COLUMN public.sales_order.cancelled
    IS 'The flag for fully cancelled orders';