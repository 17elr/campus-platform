package com.campus.notice;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController @RequestMapping("/api/notices")
public class NoticeController {
 private final JdbcTemplate db;
 public NoticeController(JdbcTemplate db) { this.db = db; }
 @GetMapping public Map<String,Object> list() {
  return Map.of("code", 200, "message", "操作成功", "data", db.queryForList("SELECT title,content,created_at AS createdAt FROM notice WHERE status=1 ORDER BY created_at DESC,id DESC LIMIT 10"));
 }
}
