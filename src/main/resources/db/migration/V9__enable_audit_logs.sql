DROP TABLE "public".audit_log;

CREATE TABLE "public".audit_log
(
    id             uuid         NOT NULL,
    sales_order_id uuid,
    "action"       varchar(255) NOT NULL,
    data           json         NOT NULL,
    created_at     timestamptz  NOT NULL DEFAULT current_timestamp
);