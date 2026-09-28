package com.campus.config;
public record OperationLogEvent(long userId, String action, long targetId, String detail) {}
