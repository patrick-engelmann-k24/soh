CREATE  TABLE "public".audit_log ( 
	id                   uuid  NOT NULL ,
	entity               varchar(100)  NOT NULL ,
	entity_id            uuid  NOT NULL ,
	"action"             varchar(20)   ,
	origin_data          text  NOT NULL ,
	created_by           varchar(255)   ,
	created_date         timestamp DEFAULT current_timestamp NOT NULL ,
	last_modified_by     varchar(255)   ,
	last_modified_date   timestamptz DEFAULT current_timestamp  ,
	CONSTRAINT pk_audit_log_id PRIMARY KEY ( id )
 );

COMMENT ON TABLE "public".audit_log IS 'stores changes in the database';

COMMENT ON COLUMN "public".audit_log.entity_id IS 'the uuid of the origin entity';

COMMENT ON COLUMN "public".audit_log."action" IS 'which type of event: create/update/delete';

COMMENT ON COLUMN "public".audit_log.created_by IS 'who was responsible for the change (IAM role?)';

CREATE  TABLE "public".sales_order ( 
	id                   uuid  NOT NULL ,
	order_number         varchar(100)  NOT NULL ,
	process_id           uuid   ,
	original_order       json   ,
	customer_email       varchar(255)   ,
	customer_number      varchar(255)   ,
	sales_channel        varchar(255)   ,
	sales_locale         varchar(10)  NOT NULL ,
	offer_reference_number varchar(255)   ,
	created_at           timestamptz DEFAULT current_timestamp  ,
	updated_at           timestamptz DEFAULT current_timestamp  ,
	CONSTRAINT pk_sales_order_id PRIMARY KEY ( id ),
	CONSTRAINT uidx_sales_order_order_number UNIQUE ( order_number ) ,
	CONSTRAINT uidx_sales_order_process_id UNIQUE ( process_id ) 
 );

COMMENT ON COLUMN "public".sales_order.process_id IS 'Camunda BPM processId';

COMMENT ON COLUMN "public".sales_order.original_order IS 'plain order json';

COMMENT ON COLUMN "public".sales_order.sales_locale IS 'origin locale of the order.\n\nATTENTION: In the legacy data are some orders which only have "DE" instead of "de_de"';

CREATE  TABLE "public".sales_order_address ( 
	id                   uuid  NOT NULL ,
	sales_order_id       uuid   ,
	address_type         varchar(50)   ,
	first_name           varchar(100)   ,
	last_name            varchar(100)   ,
	phone_number         varchar(25)   ,
	company              varchar(255)   ,
	street1              varchar(255)   ,
	street2              varchar(255)   ,
	street3              varchar(255)   ,
	city                 varchar(255)   ,
	zip_code             varchar(20)   ,
	tax_number           varchar(30)   ,
	created_at           timestamptz DEFAULT current_timestamp  ,
	updated_at           timestamptz DEFAULT current_timestamp  ,
	CONSTRAINT pk_sales_order_address_id PRIMARY KEY ( id )
 );

COMMENT ON TABLE "public".sales_order_address IS 'stores addresses for a SalesOrder';

COMMENT ON COLUMN "public".sales_order_address.address_type IS 'we should only receive "billing" or "shipping"; but to be future proof, we accept all values here.';

CREATE  TABLE "public".sales_order_invoice ( 
	id                   uuid  NOT NULL ,
	sales_order_id       uuid  NOT NULL ,
	invoice_number       varchar(255)   ,
	url                  varchar(255)   ,
	customer_access_token varchar(255)   ,
	created_at           timestamptz DEFAULT current_timestamp  ,
	updated_at           timestamptz DEFAULT current_date  ,
	CONSTRAINT pk_sasles_order_invoice_id PRIMARY KEY ( id )
 );

COMMENT ON COLUMN "public".sales_order_invoice.url IS 'url to access to invoice document (S3)';

CREATE  TABLE "public".sales_order_item ( 
	id                   uuid  NOT NULL ,
	sales_order_id       uuid  NOT NULL ,
	quantity             numeric(8,2)   ,
	stock_keeping_unit   varchar(255)   ,
	returned_at          timestamptz   ,
	delivered_at         timestamptz   ,
	cancellation_at      timestamptz   ,
	shipping_type        varchar(50)   ,
	tracking_id          varchar(255)   ,
	CONSTRAINT pk_sales_order_item_id PRIMARY KEY ( id )
 );

COMMENT ON COLUMN "public".sales_order_item.shipping_type IS 'varchar for safety. Enum could be to unflexible';

ALTER TABLE "public".sales_order_address ADD CONSTRAINT fk_sales_order_address_sales_order FOREIGN KEY ( sales_order_id ) REFERENCES "public".sales_order( id ) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE "public".sales_order_invoice ADD CONSTRAINT fk_sasles_order_invoice_sales_order FOREIGN KEY ( sales_order_id ) REFERENCES "public".sales_order( id ) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE "public".sales_order_item ADD CONSTRAINT fk_sales_order_item_sales_order FOREIGN KEY ( sales_order_id ) REFERENCES "public".sales_order( id ) ON DELETE RESTRICT ON UPDATE RESTRICT;

