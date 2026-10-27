package com.campus.controller;

import com.campus.common.Paging;
import com.campus.common.Result;
import com.campus.mapper.UserMapper;
import com.campus.security.Tokens;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/operation-logs")
public class OperationLogController {

  private static final Set<String> ACTIONS = Set.of(
    "DELIVERY_CLAIMED",
    "DELIVERY_STATUS",
    "DELIVERY_DISPUTED",
    "DELIVERY_RESOLVED",
    "COURIER_REVIEW"
  );
  private final JdbcTemplate db;
  private final Tokens tokens;
  private final UserMapper users;

  public OperationLogController(
    JdbcTemplate db,
    Tokens tokens,
    UserMapper users
  ) {
    this.db = db;
    this.tokens = tokens;
    this.users = users;
  }

  @GetMapping
  public Result<?> list(
    @RequestHeader("Authorization") String header,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(required = false) String from,
    @RequestParam(required = false) String to,
    @RequestParam(required = false) String action,
    @RequestParam(required = false) String keyword
  ) {
    if (
      !header.startsWith("Bearer ") ||
      !"ADMIN".equals(
        users.selectById(tokens.verify(header.substring(7))).getRole()
      )
    ) throw new IllegalArgumentException("无管理员权限");
    if (
      action != null && !action.isBlank() && !ACTIONS.contains(action)
    ) throw new IllegalArgumentException("操作类型无效");
    var query = new StringBuilder(
      " FROM operation_log l LEFT JOIN user u ON u.id=l.user_id WHERE 1=1"
    );
    var args = new ArrayList<Object>();
    if (from != null && !from.isBlank()) {
      query.append(" AND l.created_at>=?");
      args.add(Timestamp.valueOf(LocalDate.parse(from).atStartOfDay()));
    }
    if (to != null && !to.isBlank()) {
      query.append(" AND l.created_at<?");
      args.add(
        Timestamp.valueOf(LocalDate.parse(to).plusDays(1).atStartOfDay())
      );
    }
    if (action != null && !action.isBlank()) {
      query.append(" AND l.action=?");
      args.add(action);
    }
    if (keyword != null && !keyword.isBlank()) {
      if (keyword.length() > 100) throw new IllegalArgumentException(
        "关键词不能超过100字"
      );
      query.append(
        " AND (u.username LIKE ? OR l.detail LIKE ? OR CAST(l.target_id AS CHAR) LIKE ?)"
      );
      String term = "%" + keyword.trim() + "%";
      args.add(term);
      args.add(term);
      args.add(term);
    }
    var paging = new Paging(page, size);
    long total = db.queryForObject(
      "SELECT COUNT(*)" + query,
      Long.class,
      args.toArray()
    );
    var rows = db.queryForList(
      "SELECT l.id,l.event_id AS eventId,l.user_id AS userId,u.username,l.action,l.target_id AS targetId,l.detail,l.created_at AS createdAt" +
        query +
        " ORDER BY l.created_at DESC,l.id DESC " +
        paging.limit(),
      args.toArray()
    );
    long pending = db.queryForObject(
      "SELECT COUNT(*) FROM operation_log_outbox",
      Long.class
    );
    return Result.ok(
      Map.of(
        "records",
        rows,
        "total",
        total,
        "page",
        paging.page(),
        "size",
        paging.size(),
        "pending",
        pending
      )
    );
  }
}
