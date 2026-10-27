package com.campus.service;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class PlatformSettingService {

  final JdbcTemplate db;

  PlatformSettingService(JdbcTemplate d) {
    db = d;
  }

  public Map<String, String> all() {
    var out = new HashMap<String, String>();
    db.query("SELECT setting_key,setting_value FROM platform_setting", r -> {
      out.put(r.getString(1), r.getString(2));
    });
    return out;
  }

  public void save(Map<String, String> x, long uid) {
    x.forEach((k, v) ->
      db.update(
        "INSERT INTO platform_setting(setting_key,setting_value,updated_by) VALUES(?,?,?) ON DUPLICATE KEY UPDATE setting_value=VALUES(setting_value),updated_by=VALUES(updated_by)",
        k,
        v,
        uid
      )
    );
  }

  public String get(String k, String d) {
    try {
      return db.queryForObject(
        "SELECT setting_value FROM platform_setting WHERE setting_key=?",
        String.class,
        k
      );
    } catch (Exception e) {
      return d;
    }
  }
}
