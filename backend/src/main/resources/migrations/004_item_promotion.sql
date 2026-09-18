CREATE TABLE IF NOT EXISTS item_promotion (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 order_no VARCHAR(40) NOT NULL UNIQUE,
 item_id BIGINT NOT NULL, user_id BIGINT NOT NULL,
 promotion_type VARCHAR(30) NOT NULL, amount DECIMAL(10,2) NOT NULL,
 start_at DATETIME NULL, end_at DATETIME NULL,
 status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, paid_at DATETIME NULL,
 INDEX idx_promotion_item(item_id,status,end_at),
 INDEX idx_promotion_user(user_id,created_at,id),
 INDEX idx_promotion_paid(paid_at),
 FOREIGN KEY(item_id) REFERENCES item(id), FOREIGN KEY(user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
