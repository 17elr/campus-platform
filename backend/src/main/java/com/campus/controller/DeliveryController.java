package com.campus.controller;

import com.campus.common.Result;
import com.campus.mapper.UserMapper;
import com.campus.security.Tokens;
import com.campus.service.DeliveryService;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/delivery")
public class DeliveryController {

  private final DeliveryService service;
  private final Tokens tokens;
  private final UserMapper users;

  public DeliveryController(
    DeliveryService service,
    Tokens tokens,
    UserMapper users
  ) {
    this.service = service;
    this.tokens = tokens;
    this.users = users;
  }

  private long me(String header) {
    if (
      header == null || !header.startsWith("Bearer ")
    ) throw new IllegalArgumentException("请先登录");
    long id = tokens.verify(header.substring(7));
    var user = users.selectById(id);
    if (
      user == null || !Integer.valueOf(1).equals(user.getStatus())
    ) throw new IllegalArgumentException("账号不可用");
    return id;
  }

  private long admin(String header) {
    long id = me(header);
    if (
      !"ADMIN".equals(users.selectById(id).getRole())
    ) throw new IllegalArgumentException("无管理员权限");
    return id;
  }

  record Create(
    Long tradeOrderId,
    String deliveryType,
    String pickupLocation,
    String deliveryLocation,
    String requesterNote
  ) {}

  @PostMapping
  public Result<?> create(
    @RequestHeader("Authorization") String header,
    @RequestBody Create request
  ) {
    return Result.ok(
      service.create(
        me(header),
        request.tradeOrderId(),
        request.deliveryType(),
        request.pickupLocation(),
        request.deliveryLocation(),
        request.requesterNote()
      )
    );
  }

  @GetMapping("/public")
  public Result<?> publicList(
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(required = false) String type
  ) {
    return Result.ok(service.publicList(page, size, type));
  }

  @GetMapping
  public Result<?> list(
    @RequestHeader("Authorization") String header,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size
  ) {
    return Result.ok(service.list(me(header), page, size));
  }

  @GetMapping("/available")
  public Result<?> available(
    @RequestHeader("Authorization") String header,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(required = false) String type
  ) {
    return Result.ok(service.available(me(header), page, size, type));
  }

  @PostMapping("/{id}/claim")
  public Result<?> claim(
    @RequestHeader("Authorization") String header,
    @PathVariable long id
  ) {
    return Result.ok(service.claim(me(header), id));
  }

  @PutMapping("/{id}/status")
  public Result<?> status(
    @RequestHeader("Authorization") String header,
    @PathVariable long id,
    @RequestBody Map<String, String> request
  ) {
    return Result.ok(service.transition(me(header), id, request.get("status")));
  }

  @PostMapping("/{id}/dispute")
  public Result<?> dispute(
    @RequestHeader("Authorization") String header,
    @PathVariable long id,
    @RequestBody Map<String, String> request
  ) {
    return Result.ok(service.dispute(me(header), id, request.get("reason")));
  }

  @GetMapping("/courier/me")
  public Result<?> courier(@RequestHeader("Authorization") String header) {
    return Result.ok(service.courierApplication(me(header)));
  }

  @PostMapping("/courier/applications")
  public Result<?> apply(
    @RequestHeader("Authorization") String header,
    @RequestBody Map<String, String> request
  ) {
    return Result.ok(
      service.applyCourier(
        me(header),
        request.get("campusId"),
        request.get("note")
      )
    );
  }

  @GetMapping("/admin/couriers")
  public Result<?> couriers(
    @RequestHeader("Authorization") String header,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size
  ) {
    admin(header);
    return Result.ok(service.courierApplications(page, size));
  }

  @PutMapping("/admin/couriers/{id}")
  public Result<?> review(
    @RequestHeader("Authorization") String header,
    @PathVariable long id,
    @RequestBody Map<String, String> request
  ) {
    service.reviewCourier(
      admin(header),
      id,
      request.get("status"),
      request.get("note")
    );
    return Result.ok(null);
  }

  @PutMapping("/admin/orders/{id}/dispute")
  public Result<?> resolve(
    @RequestHeader("Authorization") String header,
    @PathVariable long id,
    @RequestBody Map<String, String> request
  ) {
    return Result.ok(
      service.resolveDispute(
        admin(header),
        id,
        request.get("action"),
        request.get("note")
      )
    );
  }

  @GetMapping("/admin/orders")
  public Result<?> all(
    @RequestHeader("Authorization") String header,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(required = false) String type
  ) {
    admin(header);
    return Result.ok(service.adminList(page, size, type));
  }

  @GetMapping("/admin/stats")
  public Result<?> stats(@RequestHeader("Authorization") String header) {
    admin(header);
    return Result.ok(service.stats());
  }
}
