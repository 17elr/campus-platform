package com.campus.service;

import java.math.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommissionService {

  private final JdbcTemplate db;

  public CommissionService(JdbcTemplate d) {
    db = d;
  }

  public record Rule(
    long id,
    BigDecimal min,
    BigDecimal max,
    BigDecimal rate,
    BigDecimal cap
  ) {}

  public Rule resolve(BigDecimal amount) {
    var r = db.queryForList(
      "SELECT id,min_amount,max_amount,rate,max_fee FROM commission_rule WHERE status=1 AND min_amount<=? AND (max_amount IS NULL OR ?<max_amount) ORDER BY min_amount DESC LIMIT 1",
      amount,
      amount
    );
    if (r.isEmpty()) throw new IllegalArgumentException("佣金规则未配置");
    var x = r.get(0);
    return new Rule(
      ((Number) x.get("id")).longValue(),
      (BigDecimal) x.get("min_amount"),
      (BigDecimal) x.get("max_amount"),
      (BigDecimal) x.get("rate"),
      (BigDecimal) x.get("max_fee")
    );
  }

  public BigDecimal fee(BigDecimal amount, Rule r) {
    return amount
      .multiply(r.rate())
      .min(r.cap())
      .setScale(2, RoundingMode.HALF_UP);
  }

  public List<Map<String, Object>> rules() {
    return db.queryForList(
      "SELECT id,min_amount minAmount,max_amount maxAmount,rate,max_fee maxFee,status,created_at createdAt FROM commission_rule ORDER BY min_amount"
    );
  }

  public List<Map<String, Object>> history() {
    return db.queryForList(
      "SELECT id,rule_id ruleId,action,min_amount minAmount,max_amount maxAmount,rate,max_fee maxFee,created_at createdAt,created_by createdBy FROM commission_rule_history ORDER BY created_at DESC,id DESC"
    );
  }

  @Transactional
  public void replace(List<Map<String, Object>> input, long uid) {
    if (input == null || input.isEmpty()) throw new IllegalArgumentException(
      "至少配置一条佣金规则"
    );
    db.update("UPDATE commission_rule SET status=0 WHERE status=1");
    for (var x : input) {
      BigDecimal min = new BigDecimal(x.get("minAmount").toString()),
        rate = new BigDecimal(x.get("rate").toString()),
        cap = new BigDecimal(x.get("maxFee").toString());
      BigDecimal max =
        x.get("maxAmount") == null || x.get("maxAmount").toString().isBlank()
          ? null
          : new BigDecimal(x.get("maxAmount").toString());
      if (
        min.signum() < 0 ||
        rate.signum() < 0 ||
        rate.compareTo(BigDecimal.ONE) > 0 ||
        cap.signum() < 0 ||
        (max != null && max.compareTo(min) <= 0)
      ) throw new IllegalArgumentException("佣金规则参数无效");
      db.update(
        "INSERT INTO commission_rule(min_amount,max_amount,rate,max_fee,status,created_by) VALUES(?,?,?,?,1,?)",
        min,
        max,
        rate,
        cap,
        uid
      );
      db.update(
        "INSERT INTO commission_rule_history(rule_id,action,min_amount,max_amount,rate,max_fee,created_by) VALUES(LAST_INSERT_ID(),'CREATE',?,?,?,?,?)",
        min,
        max,
        rate,
        cap,
        uid
      );
    }
  }
}
