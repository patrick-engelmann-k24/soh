CREATE TABLE "public".dropshipment_order_row (
                                                   id                         uuid DEFAULT gen_random_uuid () ,
                                                   sku                        varchar(100)  NOT NULL ,
                                                   order_number               varchar(100)  NOT NULL ,
                                                   quantity integer NOT NULL,
                                                   quantity_shipped integer NOT NULL DEFAULT 0,
                                                   CONSTRAINT pk_dropshipment_order_row_id PRIMARY KEY ( id )
);

-- Add order_number index if not already exists
CREATE INDEX dropshipment_order_row_order_number_idx ON public.dropshipment_order_row (order_number);

COMMENT ON INDEX dropshipment_order_row_order_number_idx IS 'An index on dropshipment_order_row order number';