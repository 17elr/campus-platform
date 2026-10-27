CREATE TABLE IF NOT EXISTS delivery_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  delivery_no VARCHAR(40) NOT NULL UNIQUE,
  trade_order_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  receiver_id BIGINT NOT NULL,
  pickup_location VARCHAR(200) NOT NULL,
  delivery_location VARCHAR(200) NOT NULL,
  fee DECIMAL(10, 2) NOT NULL,
  platform_fee DECIMAL(10, 2) NOT NULL,
  courier_id BIGINT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at DATETIME NULL,
  INDEX idx_delivery_user (sender_id, receiver_id, created_at),
  INDEX idx_delivery_status (status)
);
