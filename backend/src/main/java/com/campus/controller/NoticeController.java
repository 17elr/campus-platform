package com.campus.controller;

import com.campus.client.NoticeServiceClient;
import com.campus.common.Result;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {

  private final NoticeServiceClient notices;

  public NoticeController(NoticeServiceClient notices) {
    this.notices = notices;
  }

  @GetMapping
  @Cacheable("notices")
  public Result<?> list() {
    Map<String, Object> response = notices.list();
    return Result.ok(response.getOrDefault("data", List.of()));
  }
}
