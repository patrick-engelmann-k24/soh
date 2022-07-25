create table public.invoice_number_counter
(
    year                 int                              not null,
    counter              bigint                           not null,
    CONSTRAINT pk_invoice_number_counter_year PRIMARY KEY (year)
);