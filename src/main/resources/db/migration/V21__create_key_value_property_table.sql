CREATE  TABLE "public".key_value_property (
    id           bigint,
	key          varchar(100)  NOT NULL,
	value        varchar(100),
    created_at   timestamptz   NOT NULL DEFAULT current_timestamp,
    updated_at   timestamptz   NOT NULL DEFAULT current_timestamp,
	CONSTRAINT pk_app_property_id PRIMARY KEY (id),
    CONSTRAINT uidx_app_property_name UNIQUE (key)
 );

COMMENT ON TABLE "public".key_value_property IS 'stores key value pairs in the database';

CREATE SEQUENCE id_generator_sequence;