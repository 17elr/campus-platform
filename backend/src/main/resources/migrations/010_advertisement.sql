CREATE TABLE IF NOT EXISTS advertisement (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_name VARCHAR(100) NOT NULL,
  title VARCHAR(150) NOT NULL,
  image_url VARCHAR(500),
  target_url VARCHAR(500),
  placement VARCHAR(30) NOT NULL,
  amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  start_at DATETIME,
  end_at DATETIME,
  impressions BIGINT NOT NULL DEFAULT 0,
  clicks BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  INDEX idx_ad_active (status, placement, start_at, end_at)
);
