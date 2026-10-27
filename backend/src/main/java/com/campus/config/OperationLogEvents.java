package com.campus.config;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class OperationLogConsumer {

  private final JdbcTemplate db;

  OperationLogConsumer(JdbcTemplate db) {
    this.db = db;
  }

  @RabbitListener(
    queues = "campus.operation-log",
    autoStartup = "${campus.mq.enabled:false}"
  )
  @Transactional
  public void consume(OperationLogEvent event) {
    db.update(
      "INSERT INTO operation_log(event_id,user_id,action,target_id,detail,created_at) VALUES(?,?,?,?,?,COALESCE(?,NOW())) ON DUPLICATE KEY UPDATE event_id=event_id",
      event.eventId(),
      event.userId(),
      event.action(),
      event.targetId(),
      event.detail(),
      event.createdAt()
    );
    if (event.eventId() != null) db.update(
      "DELETE FROM operation_log_outbox WHERE event_id=?",
      event.eventId()
    );
  }
}
