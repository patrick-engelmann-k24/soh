ALTER TABLE public.sales_order
    ADD COLUMN order_group_id character varying(256);

COMMENT ON COLUMN public.sales_order.order_group_id
    IS 'The Group Id for several sales orders';
