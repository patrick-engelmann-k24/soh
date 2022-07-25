create table public.invoice_number_counter
(
    year                 smallint                         not null,
    counter              bigint                           not null,
    CONSTRAINT pk_invoice_number_counter_year PRIMARY KEY (year)
);