package com.campus.config;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class OperationLogConsumer {
 private final JdbcTemplate db;
 OperationLogConsumer(JdbcTemplate db) { this.db = db; }
 @RabbitListener(queues = "campus.operation-log", autoStartup = "${campus.mq.enabled:false}")
 public void consume(OperationLogEvent event) { db.update("INSERT INTO operation_log(user_id,action,target_id,detail) VALUES(?,?,?,?)", event.userId(), event.action(), event.targetId(), event.detail()); }
}
