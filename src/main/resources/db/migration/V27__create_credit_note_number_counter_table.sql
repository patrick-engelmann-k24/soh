create table public.credit_note_number_counter
(
    year                 integer                          not null,
    counter              bigint                           not null,
    CONSTRAINT pk_credit_note_number_counter_year PRIMARY KEY (year)
);