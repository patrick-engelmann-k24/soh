-- Prevent updates on column 'original_order'
create function prevent_update()
    returns trigger
as
$$
begin
    raise exception 'Updating the original_order column is not allowed.';
end;
$$
    language plpgsql;

create trigger prevent_update_trigger
    before update of original_order on "public".sales_order
execute procedure prevent_update();

-- Drop unnecessary columns
ALTER TABLE "public".sales_order DROP COLUMN customer_number;
ALTER TABLE "public".sales_order DROP COLUMN sales_locale;
ALTER TABLE "public".sales_order DROP COLUMN offer_reference_number;

--  Add column 'latest_json'
ALTER TABLE "public".sales_order ADD COLUMN latest_json json NOT NULL DEFAULT '{}';
UPDATE "public".sales_order SET latest_json = original_order;
ALTER TABLE "public".sales_order ALTER COLUMN latest_json DROP DEFAULT;

-- Add customer_email index
CREATE INDEX customer_email_idx ON "public".sales_order(customer_email);
