package com.campus.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.Map;

@FeignClient(name = "notice-service", url = "${notice.service.url:http://localhost:8080}")
public interface NoticeServiceClient {
 @GetMapping("/api/notices") Map<String, Object> list();
}
