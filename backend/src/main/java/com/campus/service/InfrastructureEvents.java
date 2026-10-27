package com.campus.service;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class InfrastructureEvents {

  private final JdbcTemplate db;

  @Value("${campus.mq.enabled:false}")
  private boolean enabled;

  public InfrastructureEvents(JdbcTemplate db) {
    this.db = db;
  }

  public void delivery(
    long userId,
    long orderId,
    String action,
    String detail
  ) {
    String table = enabled ? "operation_log_outbox" : "operation_log";
    db.update(
      "INSERT INTO " +
        table +
        "(event_id,user_id,action,target_id,detail) VALUES(?,?,?,?,?)",
      UUID.randomUUID().toString(),
      userId,
      action,
      orderId,
      detail
    );
  }
}
