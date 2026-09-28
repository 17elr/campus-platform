package com.campus.service;

import com.campus.common.Paging;
import java.math.BigDecimal;
import java.net.URI;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

@Service
public class AdvertisementService {
 private static final Set<String> PLACEMENTS = Set.of("HOME_BANNER", "SIDEBAR", "CATEGORY_TOP", "DETAIL_RECOMMEND");
 private final JdbcTemplate db;

 public AdvertisementService(JdbcTemplate db) { this.db = db; }

 public List<Map<String, Object>> active(String placement) {
  if (placement != null && !PLACEMENTS.contains(placement)) throw new IllegalArgumentException("广告位置无效");
  String filter = placement == null ? "" : " AND placement=?";
  Object[] args = placement == null ? new Object[0] : new Object[] {placement};
  return db.queryForList("SELECT id,merchant_name AS merchantName,title,image_url AS imageUrl,target_url AS targetUrl,placement FROM advertisement WHERE status='ACTIVE' AND (start_at IS NULL OR start_at<=NOW()) AND (end_at IS NULL OR end_at>NOW())" + filter + " ORDER BY id DESC", args);
 }

 public void impression(long id) { db.update("UPDATE advertisement SET impressions=impressions+1 WHERE id=? AND status='ACTIVE' AND (start_at IS NULL OR start_at<=NOW()) AND (end_at IS NULL OR end_at>NOW())", id); }
 public void click(long id) { db.update("UPDATE advertisement SET clicks=clicks+1 WHERE id=? AND status='ACTIVE' AND (start_at IS NULL OR start_at<=NOW()) AND (end_at IS NULL OR end_at>NOW())", id); }

 public Map<String, Object> list(int page, int size) {
  var p = new Paging(page, size);
  return Map.of("records", db.queryForList("SELECT * FROM advertisement WHERE status<>'DELETED' ORDER BY created_at DESC,id DESC " + p.limit()),
   "total", db.queryForObject("SELECT COUNT(*) FROM advertisement WHERE status<>'DELETED'", Long.class), "page", p.page(), "size", p.size());
 }

 private record Draft(String merchant, String title, String image, String target, String placement, BigDecimal amount, LocalDateTime start, LocalDateTime end) {}

 private Draft validate(Map<String, Object> input) {
  String title = value(input, "title");
  String merchant = value(input, "merchantName");
  String placement = value(input, "placement");
  String target = value(input, "targetUrl");
  if (title.isBlank() || title.length() > 150 || merchant.isBlank() || merchant.length() > 100 || !PLACEMENTS.contains(placement))
   throw new IllegalArgumentException("请填写有效的广告信息");
  if (!target.isBlank()) {
   try { var uri = URI.create(target); if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null) throw new IllegalArgumentException("广告链接须为 HTTP 或 HTTPS 地址"); }
   catch (Exception e) { throw new IllegalArgumentException("广告链接须为 HTTP 或 HTTPS 地址"); }
  }
  String image = value(input, "imageUrl");
  if (!image.isBlank() && !image.matches("/api/images/[a-f0-9-]{36}\\.png")) throw new IllegalArgumentException("请上传有效的广告图片");
  BigDecimal amount;
  try { amount = new BigDecimal(value(input, "amount").isBlank() ? "0" : value(input, "amount")); }
  catch (NumberFormatException e) { throw new IllegalArgumentException("广告金额无效"); }
  if (amount.signum() < 0 || amount.scale() > 2 || amount.compareTo(new BigDecimal("99999999")) > 0) throw new IllegalArgumentException("广告金额无效");
  LocalDateTime start = date(input, "startAt");
  LocalDateTime end = date(input, "endAt");
  if (start != null && end != null && !end.isAfter(start)) throw new IllegalArgumentException("结束时间须晚于开始时间");
  return new Draft(merchant, title, image.isBlank() ? null : image, target.isBlank() ? null : target, placement, amount, start, end);
 }

 public long create(long uid, Map<String, Object> input) {
  Draft ad = validate(input);
  var keys = new GeneratedKeyHolder();
  db.update(connection -> {
   var statement = connection.prepareStatement("INSERT INTO advertisement(merchant_name,title,image_url,target_url,placement,amount,start_at,end_at,created_by) VALUES(?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
   statement.setString(1, ad.merchant());
   statement.setString(2, ad.title());
   statement.setString(3, ad.image());
   statement.setString(4, ad.target());
   statement.setString(5, ad.placement());
   statement.setBigDecimal(6, ad.amount());
   statement.setTimestamp(7, ad.start() == null ? null : Timestamp.valueOf(ad.start()));
   statement.setTimestamp(8, ad.end() == null ? null : Timestamp.valueOf(ad.end()));
   statement.setLong(9, uid);
   return statement;
  }, keys);
  if (keys.getKey() == null) throw new IllegalStateException("广告创建失败");
  return keys.getKey().longValue();
 }

 public void update(long id, Map<String, Object> input) {
  Draft ad = validate(input);
  int changed = db.update("UPDATE advertisement SET merchant_name=?,title=?,image_url=?,target_url=?,placement=?,amount=?,start_at=?,end_at=? WHERE id=? AND status<>'DELETED'",
   ad.merchant(), ad.title(), ad.image(), ad.target(), ad.placement(), ad.amount(), ad.start(), ad.end(), id);
  if (changed != 1) throw new IllegalArgumentException("广告不存在");
 }

 public void delete(long id) {
  if (db.update("UPDATE advertisement SET status='DELETED' WHERE id=? AND status<>'DELETED'", id) != 1)
   throw new IllegalArgumentException("广告不存在");
 }

 private String value(Map<String, Object> input, String key) { return input.get(key) == null ? "" : input.get(key).toString().trim(); }
 private LocalDateTime date(Map<String, Object> input, String key) {
  String raw = value(input, key);
  if (raw.isBlank()) return null;
  try { return LocalDateTime.parse(raw); }
  catch (Exception e) { throw new IllegalArgumentException("广告日期格式无效"); }
 }

 public Map<String, Object> stats() {
  return db.queryForMap("SELECT COUNT(*) AS ads,COALESCE(SUM(CASE WHEN status='ACTIVE' THEN amount ELSE 0 END),0) AS revenue,COALESCE(SUM(impressions),0) AS impressions,COALESCE(SUM(clicks),0) AS clicks FROM advertisement WHERE status<>'DELETED'");
 }

 public void status(long id, String status) {
  if (!Set.of("ACTIVE", "PAUSED", "REJECTED").contains(status)) throw new IllegalArgumentException("广告状态无效");
  if (db.update("UPDATE advertisement SET status=? WHERE id=? AND status<>'DELETED'", status, id) != 1) throw new IllegalArgumentException("广告不存在");
 }
}
