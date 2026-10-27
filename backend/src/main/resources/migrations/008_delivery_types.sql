ALTER TABLE delivery_order
ADD COLUMN delivery_type VARCHAR(30) NOT NULL DEFAULT 'ERRAND' AFTER trade_order_id;
