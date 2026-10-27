package com.campus.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "notice-service")
public interface NoticeServiceClient {
  @GetMapping("/api/notices")
  Map<String, Object> list();
}
