package com.campus.service;

import com.campus.common.Paging;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryService {

  private static final Set<String> TYPES = Set.of("EXPRESS", "ERRAND", "FOOD");
  private final JdbcTemplate db;
  private final PlatformSettingService settings;
  private final InfrastructureEvents events;

  public DeliveryService(
    JdbcTemplate db,
    PlatformSettingService settings,
    InfrastructureEvents events
  ) {
    this.db = db;
    this.settings = settings;
    this.events = events;
  }

  public Map<String, Object> create(
    long uid,
    Long trade,
    String type,
    String pickup,
    String destination,
    String note
  ) {
    if (!TYPES.contains(type)) throw new IllegalArgumentException(
      "配送类型无效"
    );
    if (
      pickup == null ||
      pickup.isBlank() ||
      pickup.length() > 200 ||
      destination == null ||
      destination.isBlank() ||
      destination.length() > 200
    ) throw new IllegalArgumentException("请填写有效的取货和送达地点");
    if (note != null && note.length() > 500) throw new IllegalArgumentException(
      "备注不能超过500字"
    );
    long sender = uid;
    long receiver = uid;
    if (trade != null) {
      var trades = db.queryForList(
        "SELECT buyer_id,seller_id,status FROM trade_order WHERE id=?",
        trade
      );
      if (trades.isEmpty()) throw new IllegalArgumentException(
        "交易订单不存在"
      );
      var row = trades.get(0);
      long buyer = ((Number) row.get("buyer_id")).longValue();
      long seller = ((Number) row.get("seller_id")).longValue();
      if (uid != buyer && uid != seller) throw new IllegalArgumentException(
        "无权创建配送"
      );
      if (!"PAID".equals(row.get("status"))) throw new IllegalArgumentException(
        "交易订单尚未支付"
      );
      if (
        db.queryForObject(
          "SELECT COUNT(*) FROM delivery_order WHERE trade_order_id=? AND status NOT IN ('CANCELLED','COMPLETED')",
          Long.class,
          trade
        ) > 0
      ) throw new IllegalArgumentException("该交易已有配送订单");
      sender = seller;
      receiver = buyer;
    }
    BigDecimal fee = new BigDecimal(settings.get("delivery.fee", "5.00"));
    BigDecimal platformFee = fee
      .multiply(new BigDecimal(settings.get("delivery.platformRate", "0.15")))
      .setScale(2, RoundingMode.HALF_UP);
    String no = "DEL" + UUID.randomUUID().toString().replace("-", "");
    long senderId = sender;
    long receiverId = receiver;
    var keys = new GeneratedKeyHolder();
    db.update(connection -> {
      var statement = connection.prepareStatement(
        "INSERT INTO delivery_order(delivery_no,trade_order_id,delivery_type,sender_id,receiver_id,pickup_location,delivery_location,requester_note,fee,platform_fee) VALUES(?,?,?,?,?,?,?,?,?,?)",
        Statement.RETURN_GENERATED_KEYS
      );
      statement.setString(1, no);
      if (trade == null) statement.setNull(2, java.sql.Types.BIGINT);
      else statement.setLong(2, trade);
      statement.setString(3, type);
      statement.setLong(4, senderId);
      statement.setLong(5, receiverId);
      statement.setString(6, pickup.trim());
      statement.setString(7, destination.trim());
      statement.setString(8, note);
      statement.setBigDecimal(9, fee);
      statement.setBigDecimal(10, platformFee);
      return statement;
    }, keys);
    if (keys.getKey() == null) throw new IllegalStateException(
      "配送订单创建失败"
    );
    return one(keys.getKey().longValue());
  }

  private Map<String, Object> one(long id) {
    var rows = db.queryForList(
      "SELECT d.*,t.order_no AS tradeOrderNo FROM delivery_order d LEFT JOIN trade_order t ON t.id=d.trade_order_id WHERE d.id=?",
      id
    );
    if (rows.isEmpty()) throw new IllegalArgumentException("配送订单不存在");
    return rows.get(0);
  }

  public Map<String, Object> list(long uid, int page, int size) {
    var p = new Paging(page, size);
    String where = " WHERE sender_id=? OR receiver_id=? OR courier_id=?";
    return Map.of(
      "records",
      db.queryForList(
        "SELECT * FROM delivery_order" +
          where +
          " ORDER BY created_at DESC,id DESC " +
          p.limit(),
        uid,
        uid,
        uid
      ),
      "total",
      db.queryForObject(
        "SELECT COUNT(*) FROM delivery_order" + where,
        Long.class,
        uid,
        uid,
        uid
      ),
      "page",
      p.page(),
      "size",
      p.size()
    );
  }

  public Map<String, Object> available(
    long uid,
    int page,
    int size,
    String type
  ) {
    requireCourier(uid);
    var p = new Paging(page, size);
    String where =
      " WHERE status='WAITING' AND sender_id<>" +
      uid +
      " AND receiver_id<>" +
      uid +
      (type == null || type.isBlank() ? "" : " AND delivery_type=?");
    Object[] args =
      type == null || type.isBlank() ? new Object[0] : new Object[] { type };
    return Map.of(
      "records",
      db.queryForList(
        "SELECT id,delivery_type AS deliveryType,pickup_location AS pickupLocation,delivery_location AS deliveryLocation,fee FROM delivery_order" +
          where +
          " ORDER BY created_at DESC,id DESC " +
          p.limit(),
        args
      ),
      "total",
      db.queryForObject(
        "SELECT COUNT(*) FROM delivery_order" + where,
        Long.class,
        args
      ),
      "page",
      p.page(),
      "size",
      p.size()
    );
  }

  public Map<String, Object> publicList(int page, int size, String type) {
    var p = new Paging(page, size);
    String where =
      type == null || type.isBlank() ? "" : " WHERE delivery_type=?";
    Object[] args = where.isEmpty() ? new Object[0] : new Object[] { type };
    return Map.of(
      "records",
      db.queryForList(
        "SELECT id,delivery_type AS deliveryType,status,created_at AS createdAt FROM delivery_order" +
          where +
          " ORDER BY created_at DESC,id DESC " +
          p.limit(),
        args
      ),
      "total",
      db.queryForObject(
        "SELECT COUNT(*) FROM delivery_order" + where,
        Long.class,
        args
      ),
      "page",
      p.page(),
      "size",
      p.size()
    );
  }

  public Map<String, Object> adminList(int page, int size, String type) {
    var p = new Paging(page, size);
    String where =
      type == null || type.isBlank() ? "" : " WHERE delivery_type=?";
    Object[] args = where.isEmpty() ? new Object[0] : new Object[] { type };
    return Map.of(
      "records",
      db.queryForList(
        "SELECT * FROM delivery_order" +
          where +
          " ORDER BY created_at DESC,id DESC " +
          p.limit(),
        args
      ),
      "total",
      db.queryForObject(
        "SELECT COUNT(*) FROM delivery_order" + where,
        Long.class,
        args
      ),
      "page",
      p.page(),
      "size",
      p.size()
    );
  }

  @Transactional
  public Map<String, Object> claim(long uid, long id) {
    requireCourier(uid);
    int changed = db.update(
      "UPDATE delivery_order SET courier_id=?,status='ACCEPTED' WHERE id=? AND status='WAITING' AND sender_id<>? AND receiver_id<>?",
      uid,
      id,
      uid,
      uid
    );
    if (changed != 1) throw new IllegalArgumentException(
      "订单已被接单或不能接自己的订单"
    );
    events.delivery(uid, id, "DELIVERY_CLAIMED", "配送员抢单");
    return one(id);
  }

  @Transactional
  public Map<String, Object> transition(long uid, long id, String next) {
    var order = one(id);
    String current = (String) order.get("status");
    long requester = ((Number) order.get("receiver_id")).longValue();
    long courier =
      order.get("courier_id") == null
        ? -1
        : ((Number) order.get("courier_id")).longValue();
    boolean allowed =
      (uid == requester &&
        (("WAITING".equals(current) && "CANCELLED".equals(next)) ||
          ("DELIVERING".equals(current) && "COMPLETED".equals(next)))) ||
      (uid == courier &&
        (("ACCEPTED".equals(current) && "PICKED_UP".equals(next)) ||
          ("PICKED_UP".equals(current) && "DELIVERING".equals(next))));
    if (!allowed) throw new IllegalArgumentException("无权执行该配送状态变更");
    int changed = db.update(
      "UPDATE delivery_order SET status=?,completed_at=CASE WHEN ?='COMPLETED' THEN NOW() ELSE completed_at END WHERE id=? AND status=?",
      next,
      next,
      id,
      current
    );
    if (changed != 1) throw new IllegalArgumentException(
      "配送状态已变化，请刷新重试"
    );
    events.delivery(uid, id, "DELIVERY_STATUS", "状态变更为" + next);
    return one(id);
  }

  public Map<String, Object> courierApplication(long uid) {
    var rows = db.queryForList(
      "SELECT id,campus_id AS campusId,note,status,review_note AS reviewNote,created_at AS createdAt,reviewed_at AS reviewedAt FROM delivery_courier_application WHERE user_id=?",
      uid
    );
    return rows.isEmpty() ? Map.of("status", "NONE") : rows.get(0);
  }

  public Map<String, Object> applyCourier(
    long uid,
    String campusId,
    String note
  ) {
    if (
      campusId == null || !campusId.trim().matches("[A-Za-z0-9_-]{4,40}")
    ) throw new IllegalArgumentException("请填写有效的校园身份编号");
    if (note != null && note.length() > 500) throw new IllegalArgumentException(
      "申请说明不能超过500字"
    );
    var current = courierApplication(uid);
    if (
      "PENDING".equals(current.get("status")) ||
      "APPROVED".equals(current.get("status"))
    ) throw new IllegalArgumentException("申请已提交或已审核通过");
    db.update(
      "INSERT INTO delivery_courier_application(user_id,campus_id,note,status) VALUES(?,?,?,'PENDING') ON DUPLICATE KEY UPDATE campus_id=VALUES(campus_id),note=VALUES(note),status='PENDING',review_note=NULL,reviewed_by=NULL,reviewed_at=NULL",
      uid,
      campusId.trim(),
      note
    );
    return courierApplication(uid);
  }

  private void requireCourier(long uid) {
    if (
      !"APPROVED".equals(courierApplication(uid).get("status"))
    ) throw new IllegalArgumentException("请先申请并通过配送员审核");
  }

  public Map<String, Object> courierApplications(int page, int size) {
    var p = new Paging(page, size);
    return Map.of(
      "records",
      db.queryForList(
        "SELECT a.id,a.user_id AS userId,u.username,u.nickname,a.campus_id AS campusId,a.note,a.status,a.review_note AS reviewNote,a.created_at AS createdAt FROM delivery_courier_application a JOIN user u ON u.id=a.user_id ORDER BY (a.status='PENDING') DESC,a.updated_at DESC,a.id DESC " +
          p.limit()
      ),
      "total",
      db.queryForObject(
        "SELECT COUNT(*) FROM delivery_courier_application",
        Long.class
      ),
      "page",
      p.page(),
      "size",
      p.size()
    );
  }

  @Transactional
  public void reviewCourier(
    long reviewer,
    long applicationId,
    String status,
    String note
  ) {
    if (
      !Set.of("APPROVED", "REJECTED").contains(status)
    ) throw new IllegalArgumentException("审核状态无效");
    if (note != null && note.length() > 500) throw new IllegalArgumentException(
      "审核说明不能超过500字"
    );
    if (
      "REJECTED".equals(status) && (note == null || note.isBlank())
    ) throw new IllegalArgumentException("驳回时请填写原因");
    int changed = db.update(
      "UPDATE delivery_courier_application SET status=?,review_note=?,reviewed_by=?,reviewed_at=NOW() WHERE id=? AND status='PENDING' AND user_id<>?",
      status,
      note,
      reviewer,
      applicationId,
      reviewer
    );
    if (changed != 1) throw new IllegalArgumentException(
      "申请不存在、已审核或不能审核自己的申请"
    );
    long applicant = db.queryForObject(
      "SELECT user_id FROM delivery_courier_application WHERE id=?",
      Long.class,
      applicationId
    );
    events.delivery(
      reviewer,
      applicationId,
      "COURIER_REVIEW",
      status +
        ": " +
        (note == null ? "" : note.trim()) +
        " (申请人ID " +
        applicant +
        ")"
    );
  }

  @Transactional
  public Map<String, Object> dispute(long uid, long id, String reason) {
    if (
      reason == null || reason.isBlank() || reason.length() > 500
    ) throw new IllegalArgumentException("请填写500字以内的异常说明");
    int changed = db.update(
      "UPDATE delivery_order SET dispute_from_status=status,status='DISPUTED',dispute_reason=?,dispute_by=?,resolution_note=NULL,resolved_at=NULL WHERE id=? AND status IN ('ACCEPTED','PICKED_UP','DELIVERING') AND (receiver_id=? OR sender_id=? OR courier_id=?)",
      reason.trim(),
      uid,
      id,
      uid,
      uid,
      uid
    );
    if (changed != 1) throw new IllegalArgumentException(
      "当前订单不能发起异常处理"
    );
    events.delivery(uid, id, "DELIVERY_DISPUTED", reason.trim());
    return one(id);
  }

  @Transactional
  public Map<String, Object> resolveDispute(
    long reviewer,
    long id,
    String action,
    String note
  ) {
    if (
      !Set.of("RESUME", "CANCEL").contains(action)
    ) throw new IllegalArgumentException("处理方式无效");
    if (
      note == null || note.isBlank() || note.length() > 500
    ) throw new IllegalArgumentException("请填写500字以内的处理说明");
    int changed = db.update(
      "UPDATE delivery_order SET status=CASE WHEN ?='CANCEL' THEN 'CANCELLED' ELSE dispute_from_status END,resolution_note=?,resolved_at=NOW() WHERE id=? AND status='DISPUTED'",
      action,
      note.trim(),
      id
    );
    if (changed != 1) throw new IllegalArgumentException(
      "异常订单不存在或已处理"
    );
    events.delivery(
      reviewer,
      id,
      "DELIVERY_RESOLVED",
      action + ": " + note.trim()
    );
    return one(id);
  }

  public Map<String, Object> stats() {
    return db.queryForMap(
      "SELECT COUNT(*) AS orders,COALESCE(SUM(CASE WHEN status='COMPLETED' THEN fee ELSE 0 END),0) AS fees,COALESCE(SUM(CASE WHEN status='COMPLETED' THEN platform_fee ELSE 0 END),0) AS platformFees,COALESCE(SUM(status='COMPLETED'),0) AS completed FROM delivery_order"
    );
  }
}
