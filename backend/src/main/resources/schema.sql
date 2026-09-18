CREATE DATABASE IF NOT EXISTS campus_platform DEFAULT CHARACTER SET utf8mb4;
USE campus_platform;
CREATE TABLE IF NOT EXISTS user (id BIGINT PRIMARY KEY AUTO_INCREMENT,username VARCHAR(50) UNIQUE NOT NULL,password VARCHAR(100) NOT NULL,nickname VARCHAR(50),role VARCHAR(20) DEFAULT 'STUDENT',status TINYINT DEFAULT 1,created_at DATETIME DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS item (id BIGINT PRIMARY KEY AUTO_INCREMENT,title VARCHAR(100) NOT NULL,description TEXT,type VARCHAR(20) NOT NULL,category VARCHAR(50),location VARCHAR(100),contact VARCHAR(100),image_url VARCHAR(255),status VARCHAR(20) DEFAULT 'PENDING',user_id BIGINT,created_at DATETIME DEFAULT CURRENT_TIMESTAMP,updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,INDEX idx_item_status(status),INDEX idx_item_type(type),INDEX idx_item_location(location));
CREATE TABLE IF NOT EXISTS favorite (id BIGINT PRIMARY KEY AUTO_INCREMENT,user_id BIGINT,item_id BIGINT,UNIQUE KEY uk_favorite(user_id,item_id));
CREATE TABLE IF NOT EXISTS report (id BIGINT PRIMARY KEY AUTO_INCREMENT,user_id BIGINT,item_id BIGINT,reason VARCHAR(255),status VARCHAR(20) DEFAULT 'PENDING',created_at DATETIME DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS trade_order (id BIGINT PRIMARY KEY AUTO_INCREMENT,order_no VARCHAR(40) UNIQUE NOT NULL,product_id BIGINT,buyer_id BIGINT,seller_id BIGINT,amount DECIMAL(10,2),commission DECIMAL(10,2),seller_amount DECIMAL(10,2),payment_method VARCHAR(20),status VARCHAR(30),created_at DATETIME DEFAULT CURRENT_TIMESTAMP,paid_at DATETIME NULL,INDEX idx_order_buyer(buyer_id),INDEX idx_order_seller(seller_id));
CREATE TABLE IF NOT EXISTS notice (id BIGINT PRIMARY KEY AUTO_INCREMENT,title VARCHAR(150) NOT NULL,content TEXT,status TINYINT DEFAULT 1,created_at DATETIME DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS operation_log (id BIGINT PRIMARY KEY AUTO_INCREMENT,user_id BIGINT,action VARCHAR(80),target_id BIGINT,detail VARCHAR(255),created_at DATETIME DEFAULT CURRENT_TIMESTAMP,INDEX idx_log_created(created_at));
INSERT IGNORE INTO user(username,password,nickname,role) VALUES ('admin','admin123','系统管理员','ADMIN');
INSERT IGNORE INTO item(title,description,type,category,location,contact,status,user_id) VALUES ('校园卡','在图书馆门口拾到校园卡，请失主联系','FOUND','证件','图书馆','admin','APPROVED',1);
INSERT IGNORE INTO notice(title,content) VALUES ('失物认领请携带有效证件','请在服务台当面核实物品信息。'),('二手交易建议选择公共区域','保护个人隐私，谨防诈骗。'),('请勿发布虚假或违规信息','共同维护友好的校园环境。');

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
