ALTER TABLE delivery_order
MODIFY trade_order_id BIGINT NULL,
ADD COLUMN requester_note VARCHAR(500) NULL AFTER delivery_location;
