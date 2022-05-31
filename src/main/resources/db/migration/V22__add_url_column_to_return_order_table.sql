ALTER TABLE "public".return_order ADD COLUMN "url" varchar(255);

COMMENT ON COLUMN "public".return_order."url" IS 'Invoıce URL for dropshipment order return';