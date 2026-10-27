package com.campus.config;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "campus.mq.enabled", havingValue = "true")
public class OperationLogPublisher {

  private static final Logger log = LoggerFactory.getLogger(
    OperationLogPublisher.class
  );
  private final JdbcTemplate db;
  private final RabbitTemplate rabbit;

  public OperationLogPublisher(JdbcTemplate db, RabbitTemplate rabbit) {
    this.db = db;
    this.rabbit = rabbit;
  }

  @Scheduled(fixedDelayString = "${campus.mq.publish-delay-ms:5000}")
  public void publish() {
    // A crash between publish and update can resend an event; event_id makes consumption idempotent.
    for (var row : db.queryForList(
      "SELECT * FROM operation_log_outbox WHERE published_at IS NULL ORDER BY created_at,event_id LIMIT 50"
    )) {
      String eventId = (String) row.get("event_id");
      var event = new OperationLogEvent(
        eventId,
        ((Number) row.get("user_id")).longValue(),
        (String) row.get("action"),
        ((Number) row.get("target_id")).longValue(),
        (String) row.get("detail"),
        row.get("created_at").toString()
      );
      try {
        var correlation = new CorrelationData(eventId);
        rabbit.convertAndSend("", "campus.operation-log", event, correlation);
        var confirm = correlation.getFuture().get(3, TimeUnit.SECONDS);
        if (
          !confirm.isAck() || correlation.getReturned() != null
        ) throw new IllegalStateException(
          "RabbitMQ did not route or acknowledge the event"
        );
        db.update(
          "UPDATE operation_log_outbox SET published_at=NOW() WHERE event_id=?",
          eventId
        );
      } catch (InterruptedException error) {
        Thread.currentThread().interrupt();
        return;
      } catch (Exception error) {
        log.warn(
          "Operation log {} remains queued for retry: {}",
          eventId,
          error.getMessage()
        );
        return;
      }
    }
  }
}
