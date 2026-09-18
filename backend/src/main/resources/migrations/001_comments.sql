CREATE TABLE IF NOT EXISTS item_comment (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 item_id BIGINT NOT NULL,user_id BIGINT NOT NULL,parent_id BIGINT NULL,
 content VARCHAR(500) NOT NULL,deleted TINYINT NOT NULL DEFAULT 0,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 INDEX idx_comment_item_time(item_id,created_at,id),
 FOREIGN KEY(item_id) REFERENCES item(id),
 FOREIGN KEY(user_id) REFERENCES user(id),
 FOREIGN KEY(parent_id) REFERENCES item_comment(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
