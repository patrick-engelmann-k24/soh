-- Add order_group_idx index if not already exists
CREATE INDEX IF NOT EXISTS order_group_idx ON public.sales_order (order_group_id);

COMMENT ON INDEX order_group_idx IS 'An index on order group column increasing query performance';
