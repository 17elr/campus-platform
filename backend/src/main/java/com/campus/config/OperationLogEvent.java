package com.campus.config;

public record OperationLogEvent(
  String eventId,
  long userId,
  String action,
  long targetId,
  String detail,
  String createdAt
) {}
