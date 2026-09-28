package com.campus.service;

import com.campus.config.OperationLogEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class InfrastructureEvents {
 private final RabbitTemplate rabbit;
 private static final Logger log = LoggerFactory.getLogger(InfrastructureEvents.class);
 @Value("${campus.mq.enabled:false}") private boolean enabled;
 public InfrastructureEvents(RabbitTemplate rabbit) { this.rabbit = rabbit; }
 public void delivery(long userId, long orderId, String action, String detail) {
  if (!enabled) return;
  try { rabbit.convertAndSend("campus.operation-log", new OperationLogEvent(userId, action, orderId, detail)); }
  catch (RuntimeException error) { log.warn("Delivery log event was not published for order {}", orderId, error); }
 }
}
