CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE "public".dropshipment_invoice_row (
                                              id                         uuid DEFAULT gen_random_uuid () ,
                                              sku                        varchar(100)  NOT NULL ,
                                              order_number               varchar(100)  NOT NULL ,
                                              invoice_number             varchar(255) ,
                                              CONSTRAINT pk_dropshipment_invoice_row_id PRIMARY KEY ( id )
);