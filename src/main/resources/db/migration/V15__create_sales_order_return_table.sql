create table public.sales_order_return
(
    id                    uuid                             not null,
    sales_order_id        uuid                             not null,
    order_number          varchar(100)                     not null,
    order_group_id        varchar(256)                             ,
    created_at            timestamp with time zone default CURRENT_TIMESTAMP,
    return_order_json     json                             not null,
    CONSTRAINT pk_sales_order_return_id PRIMARY KEY (id)
);

ALTER TABLE sales_order_return ADD CONSTRAINT fk_sales_order_return_sales_order
    FOREIGN KEY (sales_order_id) REFERENCES public.sales_order(id) ON DELETE CASCADE;

comment on column sales_order_return.return_order_json is 'Return order json';

comment on column sales_order_return.order_group_id is 'The Group Id for several sales orders';

create index sales_order_return_order_group_idx
    on sales_order_return(order_group_id);

comment on index sales_order_return_order_group_idx is 'An index on order group column increasing query performance';

create index sales_order_return_order_number_idx
    on sales_order_return(order_number);

comment on index sales_order_return_order_number_idx is 'An index on order number column increasing query performance';

create index sales_order_return_sales_order_id_idx
    on sales_order_return(sales_order_id);

comment on index sales_order_return_sales_order_id_idx is 'An index on FK reducing lookups';

create trigger sales_order_return_prevent_update_trigger
    before update
        of return_order_json
    on sales_order_return
execute procedure prevent_update();
