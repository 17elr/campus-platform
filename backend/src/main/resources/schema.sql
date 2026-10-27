CREATE DATABASE IF NOT EXISTS campus_platform DEFAULT CHARACTER SET utf8mb4;

USE campus_platform;

CREATE TABLE IF NOT EXISTS user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) UNIQUE NOT NULL,
  password VARCHAR(100) NOT NULL,
  nickname VARCHAR(50),
  role VARCHAR(20) DEFAULT 'STUDENT',
  bio VARCHAR(500),
  avatar_url VARCHAR(500),
  status TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(100) NOT NULL,
  description TEXT,
  type VARCHAR(20) NOT NULL,
  category VARCHAR(50),
  location VARCHAR(100),
  contact VARCHAR(100),
  image_url VARCHAR(255),
  status VARCHAR(20) DEFAULT 'PENDING',
  user_id BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_item_status (status),
  INDEX idx_item_type (type),
  INDEX idx_item_location (location)
);

CREATE TABLE IF NOT EXISTS favorite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  item_id BIGINT,
  UNIQUE KEY uk_favorite (user_id, item_id)
);

CREATE TABLE IF NOT EXISTS report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  item_id BIGINT,
  reason VARCHAR(255),
  status VARCHAR(20) DEFAULT 'PENDING',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS trade_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(40) UNIQUE NOT NULL,
  product_id BIGINT,
  buyer_id BIGINT,
  seller_id BIGINT,
  amount DECIMAL(10, 2),
  commission DECIMAL(10, 2),
  seller_amount DECIMAL(10, 2),
  payment_method VARCHAR(20),
  status VARCHAR(30),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  paid_at DATETIME NULL,
  commission_rule_id BIGINT NULL,
  INDEX idx_order_buyer (buyer_id),
  INDEX idx_order_seller (seller_id)
);

CREATE TABLE IF NOT EXISTS notice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(150) NOT NULL,
  content TEXT,
  status TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_id VARCHAR(36) UNIQUE,
  user_id BIGINT,
  action VARCHAR(80),
  target_id BIGINT,
  detail VARCHAR(255),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_log_created (created_at)
);

CREATE TABLE IF NOT EXISTS operation_log_outbox (
  event_id VARCHAR(36) PRIMARY KEY,
  user_id BIGINT NOT NULL,
  action VARCHAR(80) NOT NULL,
  target_id BIGINT NOT NULL,
  detail VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  published_at DATETIME NULL,
  INDEX idx_outbox_pending (published_at, created_at)
);

INSERT IGNORE INTO
  user (username, password, nickname, role)
VALUES
  ('admin', 'admin123', '系统管理员', 'ADMIN');

INSERT IGNORE INTO
  item (
    title,
    description,
    type,
    category,
    location,
    contact,
    status,
    user_id
  )
VALUES
  (
    '校园卡',
    '在图书馆门口拾到校园卡，请失主联系',
    'FOUND',
    '证件',
    '图书馆',
    'admin',
    'APPROVED',
    1
  );

INSERT IGNORE INTO
  notice (title, content)
VALUES
  ('失物认领请携带有效证件', '请在服务台当面核实物品信息。'),
  ('二手交易建议选择公共区域', '保护个人隐私，谨防诈骗。'),
  ('请勿发布虚假或违规信息', '共同维护友好的校园环境。');

CREATE TABLE IF NOT EXISTS item_comment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  item_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  parent_id BIGINT NULL,
  content VARCHAR(500) NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_comment_item_time (item_id, created_at, id),
  FOREIGN KEY (item_id) REFERENCES item (id),
  FOREIGN KEY (user_id) REFERENCES user (id),
  FOREIGN KEY (parent_id) REFERENCES item_comment (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS item_promotion (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(40) NOT NULL UNIQUE,
  item_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  promotion_type VARCHAR(30) NOT NULL,
  amount DECIMAL(10, 2) NOT NULL,
  start_at DATETIME NULL,
  end_at DATETIME NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  paid_at DATETIME NULL,
  INDEX idx_promotion_item (item_id, status, end_at),
  INDEX idx_promotion_user (user_id, created_at, id),
  INDEX idx_promotion_paid (paid_at),
  FOREIGN KEY (item_id) REFERENCES item (id),
  FOREIGN KEY (user_id) REFERENCES user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS commission_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  min_amount DECIMAL(10, 2) NOT NULL,
  max_amount DECIMAL(10, 2) NULL,
  rate DECIMAL(6, 5) NOT NULL,
  max_fee DECIMAL(10, 2) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  INDEX idx_commission_active (status, min_amount)
);

CREATE TABLE IF NOT EXISTS commission_rule_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rule_id BIGINT NULL,
  action VARCHAR(20) NOT NULL,
  min_amount DECIMAL(10, 2),
  max_amount DECIMAL(10, 2),
  rate DECIMAL(6, 5),
  max_fee DECIMAL(10, 2),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by BIGINT NULL
);

INSERT INTO
  commission_rule (min_amount, max_amount, rate, max_fee, status)
SELECT
  *
FROM
  (
    SELECT
      0.00,
      50.00,
      0.02000,
      10.00,
      1
    UNION ALL
    SELECT
      50.00,
      200.00,
      0.03000,
      15.00,
      1
    UNION ALL
    SELECT
      200.00,
      500.00,
      0.04000,
      20.00,
      1
    UNION ALL
    SELECT
      500.00,
      NULL,
      0.05000,
      30.00,
      1
  ) x
WHERE
  NOT EXISTS (
    SELECT
      1
    FROM
      commission_rule
    LIMIT
      1
  );

CREATE TABLE IF NOT EXISTS delivery_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  delivery_no VARCHAR(40) NOT NULL UNIQUE,
  trade_order_id BIGINT NULL,
  sender_id BIGINT NOT NULL,
  receiver_id BIGINT NOT NULL,
  pickup_location VARCHAR(200) NOT NULL,
  delivery_location VARCHAR(200) NOT NULL,
  requester_note VARCHAR(500) NULL,
  fee DECIMAL(10, 2) NOT NULL,
  platform_fee DECIMAL(10, 2) NOT NULL,
  courier_id BIGINT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
  dispute_reason VARCHAR(500) NULL,
  dispute_by BIGINT NULL,
  dispute_from_status VARCHAR(20) NULL,
  resolution_note VARCHAR(500) NULL,
  resolved_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at DATETIME NULL,
  INDEX idx_delivery_user (sender_id, receiver_id, created_at),
  INDEX idx_delivery_status (status)
);

CREATE TABLE IF NOT EXISTS delivery_courier_application (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  campus_id VARCHAR(40) NOT NULL,
  note VARCHAR(500) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  review_note VARCHAR(500) NULL,
  reviewed_by BIGINT NULL,
  reviewed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_courier_application_status (status, created_at)
);

CREATE TABLE IF NOT EXISTS platform_setting (
  setting_key VARCHAR(80) PRIMARY KEY,
  setting_value VARCHAR(255) NOT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by BIGINT NULL
);

INSERT IGNORE INTO
  platform_setting (setting_key, setting_value)
VALUES
  ('delivery.fee', '5.00'),
  ('delivery.platformRate', '0.15'),
  ('promotion.TOP_24H.amount', '1.99'),
  ('promotion.TOP_24H.hours', '24'),
  ('promotion.FEATURED_3D.amount', '4.99'),
  ('promotion.FEATURED_3D.hours', '72'),
  ('promotion.CATEGORY_7D.amount', '9.90'),
  ('promotion.CATEGORY_7D.hours', '168');

ALTER TABLE delivery_order
ADD COLUMN delivery_type VARCHAR(30) NOT NULL DEFAULT 'ERRAND' AFTER trade_order_id;

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
