package com.campus.service;

import com.campus.common.Paging;
import com.campus.entity.Item;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PromotionService {

  private final JdbcTemplate db;
  private final PlatformSettingService settings;

  public PromotionService(JdbcTemplate db, PlatformSettingService settings) {
    this.db = db;
    this.settings = settings;
  }

  public enum Plan {
    TOP_24H("24 小时置顶", "1.99", 24),
    FEATURED_3D("首页推荐 3 天", "4.99", 72),
    CATEGORY_7D("分类置顶 7 天", "9.90", 168);

    public final String label;
    public final BigDecimal amount;
    public final int hours;

    Plan(String label, String amount, int hours) {
      this.label = label;
      this.amount = new BigDecimal(amount);
      this.hours = hours;
    }
  }

  public List<Map<String, Object>> plans() {
    return Arrays.stream(Plan.values())
      .map(p ->
        Map.<String, Object>of(
          "type",
          p.name(),
          "label",
          p.label,
          "amount",
          new BigDecimal(
            settings.get(
              "promotion." + p.name() + ".amount",
              p.amount.toString()
            )
          ),
          "hours",
          Integer.parseInt(
            settings.get(
              "promotion." + p.name() + ".hours",
              String.valueOf(p.hours)
            )
          )
        )
      )
      .toList();
  }

  private Plan plan(String type) {
    try {
      return Plan.valueOf(type);
    } catch (Exception e) {
      throw new IllegalArgumentException("推广类型无效");
    }
  }

  private void lockItem(long itemId, long uid) {
    var rows = db.queryForList(
      "SELECT user_id,type,status FROM item WHERE id=? FOR UPDATE",
      itemId
    );
    if (
      rows.isEmpty() ||
      !Objects.equals(((Number) rows.get(0).get("user_id")).longValue(), uid)
    ) throw new IllegalArgumentException("只能推广自己的商品");
    var item = rows.get(0);
    if (
      !"SECOND_HAND".equals(item.get("type")) ||
      !"APPROVED".equals(item.get("status"))
    ) throw new IllegalArgumentException("仅可推广已公开的二手商品");
  }

  @Transactional
  public Map<String, Object> create(long uid, long itemId, String type) {
    Plan p = plan(type);
    lockItem(itemId, uid);
    if (
      db.queryForObject(
        "SELECT COUNT(*) FROM item_promotion WHERE item_id=? AND promotion_type=? AND status='ACTIVE' AND end_at>NOW()",
        Long.class,
        itemId,
        type
      ) > 0
    ) throw new IllegalArgumentException(
      "该商品已有同类型推广，请到期后再购买"
    );
    var pending = db.queryForList(
      "SELECT id FROM item_promotion WHERE item_id=? AND user_id=? AND promotion_type=? AND status='PENDING_PAYMENT' ORDER BY id DESC LIMIT 1",
      itemId,
      uid,
      type
    );
    if (!pending.isEmpty()) return order(
      ((Number) pending.get(0).get("id")).longValue()
    );
    String no = "PROMO" + UUID.randomUUID().toString().replace("-", "");
    db.update(
      "INSERT INTO item_promotion(order_no,item_id,user_id,promotion_type,amount) VALUES(?,?,?,?,?)",
      no,
      itemId,
      uid,
      type,
      p.amount
    );
    return order(
      db.queryForObject(
        "SELECT id FROM item_promotion WHERE order_no=?",
        Long.class,
        no
      )
    );
  }

  private static final String SELECT =
    "SELECT p.id,p.order_no AS orderNo,p.item_id AS itemId,p.user_id AS userId,p.promotion_type AS promotionType,p.amount,p.start_at AS startAt,p.end_at AS endAt,CASE WHEN p.status='ACTIVE' AND p.end_at<=NOW() THEN 'EXPIRED' ELSE p.status END AS status,p.created_at AS createdAt,p.paid_at AS paidAt,i.title AS itemTitle FROM item_promotion p JOIN item i ON i.id=p.item_id";

  public Map<String, Object> order(long id) {
    return db.queryForMap(SELECT + " WHERE p.id=?", id);
  }

  @Transactional
  public Map<String, Object> pay(long uid, long id) {
    var rows = db.queryForList(
      "SELECT item_id FROM item_promotion WHERE id=? AND user_id=?",
      id,
      uid
    );
    if (rows.isEmpty()) throw new IllegalArgumentException("推广订单不存在");
    long itemId = ((Number) rows.get(0).get("item_id")).longValue();
    lockItem(itemId, uid);
    var row = db.queryForMap(
      "SELECT status,promotion_type FROM item_promotion WHERE id=? FOR UPDATE",
      id
    );
    if (
      !"PENDING_PAYMENT".equals(row.get("status"))
    ) throw new IllegalArgumentException("订单已支付或已取消，请勿重复支付");
    String type = (String) row.get("promotion_type");
    if (
      db.queryForObject(
        "SELECT COUNT(*) FROM item_promotion WHERE item_id=? AND promotion_type=? AND status='ACTIVE' AND end_at>NOW()",
        Long.class,
        itemId,
        type
      ) > 0
    ) throw new IllegalArgumentException("同类型推广仍在生效");
    db.update(
      "UPDATE item_promotion SET status='ACTIVE',paid_at=NOW(),start_at=NOW(),end_at=TIMESTAMPADD(HOUR,?,NOW()) WHERE id=? AND status='PENDING_PAYMENT'",
      plan(type).hours,
      id
    );
    return order(id);
  }

  public void cancel(long uid, long id) {
    if (
      db.update(
        "UPDATE item_promotion SET status='CANCELLED' WHERE id=? AND user_id=? AND status='PENDING_PAYMENT'",
        id,
        uid
      ) != 1
    ) throw new IllegalArgumentException("只能取消自己的待付款订单");
  }

  public Map<String, Object> list(Long uid, int page, int size) {
    var p = new Paging(page, size);
    String where = uid == null ? "" : " WHERE p.user_id=?";
    Object[] args = uid == null ? new Object[] {} : new Object[] { uid };
    return Map.of(
      "records",
      db.queryForList(
        SELECT + where + " ORDER BY p.created_at DESC,p.id DESC " + p.limit(),
        args
      ),
      "total",
      db.queryForObject(
        "SELECT COUNT(*) FROM item_promotion p" + where,
        Long.class,
        args
      ),
      "page",
      p.page(),
      "size",
      p.size()
    );
  }

  public Map<String, Object> stats() {
    return db.queryForMap(
      "SELECT COALESCE(SUM(CASE WHEN paid_at IS NOT NULL THEN amount ELSE 0 END),0) AS revenue,COUNT(paid_at) AS paidOrders,COALESCE(SUM(CASE WHEN paid_at>=CURDATE() THEN amount ELSE 0 END),0) AS todayRevenue,COALESCE(SUM(CASE WHEN paid_at>=DATE_FORMAT(NOW(),'%Y-%m-01') THEN amount ELSE 0 END),0) AS monthRevenue,COALESCE(SUM(status='ACTIVE' AND start_at<=NOW() AND end_at>NOW()),0) AS activeOrders FROM item_promotion"
    );
  }

  public void decorate(List<Item> items) {
    if (items.isEmpty()) return;
    String marks = String.join(",", Collections.nCopies(items.size(), "?"));
    var rows = db.queryForList(
      "SELECT item_id,promotion_type FROM item_promotion WHERE item_id IN (" +
        marks +
        ") AND status='ACTIVE' AND start_at<=NOW() AND end_at>NOW()",
      items.stream().map(Item::getId).toArray()
    );
    for (Item item : items)
      item.setPromotions(
        rows
          .stream()
          .filter(r -> ((Number) r.get("item_id")).longValue() == item.getId())
          .map(r -> (String) r.get("promotion_type"))
          .distinct()
          .toList()
      );
  }

  public static String ranking(String scene, boolean category) {
    String types = "'TOP_24H'";
    if ("home".equals(scene)) types += ",'FEATURED_3D'";
    if (category) types += ",'CATEGORY_7D'";
    return (
      "EXISTS(SELECT 1 FROM item_promotion p WHERE p.item_id=item.id AND p.status='ACTIVE' AND p.start_at<=NOW() AND p.end_at>NOW() AND p.promotion_type IN (" +
      types +
      "))"
    );
  }
}
