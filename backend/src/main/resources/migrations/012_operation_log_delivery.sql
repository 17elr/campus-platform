USE campus_platform;

ALTER TABLE operation_log
ADD COLUMN event_id VARCHAR(36) NULL,
ADD UNIQUE KEY uk_log_event (event_id);

CREATE TABLE operation_log_outbox (
  event_id VARCHAR(36) PRIMARY KEY,
  user_id BIGINT NOT NULL,
  action VARCHAR(80) NOT NULL,
  target_id BIGINT NOT NULL,
  detail VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  published_at DATETIME NULL,
  INDEX idx_outbox_pending (published_at, created_at)
);
