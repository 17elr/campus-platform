package com.campus.controller;

import com.campus.common.Result;
import com.campus.mapper.UserMapper;
import com.campus.security.Tokens;
import com.campus.service.AdvertisementService;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/advertisements")
public class AdvertisementController {

  private final AdvertisementService ads;
  private final Tokens tokens;
  private final UserMapper users;

  public AdvertisementController(
    AdvertisementService ads,
    Tokens tokens,
    UserMapper users
  ) {
    this.ads = ads;
    this.tokens = tokens;
    this.users = users;
  }

  private long me(String header) {
    if (
      header == null || !header.startsWith("Bearer ")
    ) throw new IllegalArgumentException("请先登录");
    return tokens.verify(header.substring(7));
  }

  private void admin(String header) {
    var user = users.selectById(me(header));
    if (
      user == null ||
      !"ADMIN".equals(user.getRole()) ||
      !Integer.valueOf(1).equals(user.getStatus())
    ) throw new IllegalArgumentException("无管理员权限");
  }

  @GetMapping
  public Result<?> active(@RequestParam(required = false) String placement) {
    return Result.ok(ads.active(placement));
  }

  @PostMapping("/{id}/impression")
  public Result<?> impression(@PathVariable long id) {
    ads.impression(id);
    return Result.ok(null);
  }

  @PostMapping("/{id}/click")
  public Result<?> click(@PathVariable long id) {
    ads.click(id);
    return Result.ok(null);
  }

  @PostMapping
  public Result<?> create(
    @RequestHeader("Authorization") String header,
    @RequestBody Map<String, Object> input
  ) {
    admin(header);
    return Result.ok(Map.of("id", ads.create(me(header), input)));
  }

  @PutMapping("/{id}")
  public Result<?> update(
    @RequestHeader("Authorization") String header,
    @PathVariable long id,
    @RequestBody Map<String, Object> input
  ) {
    admin(header);
    ads.update(id, input);
    return Result.ok(null);
  }

  @DeleteMapping("/{id}")
  public Result<?> delete(
    @RequestHeader("Authorization") String header,
    @PathVariable long id
  ) {
    admin(header);
    ads.delete(id);
    return Result.ok(null);
  }

  @GetMapping("/admin/stats")
  public Result<?> stats(@RequestHeader("Authorization") String header) {
    admin(header);
    return Result.ok(ads.stats());
  }

  @GetMapping("/admin")
  public Result<?> list(
    @RequestHeader("Authorization") String header,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size
  ) {
    admin(header);
    return Result.ok(ads.list(page, size));
  }

  @PutMapping("/{id}/status")
  public Result<?> status(
    @RequestHeader("Authorization") String header,
    @PathVariable long id,
    @RequestBody Map<String, String> input
  ) {
    admin(header);
    ads.status(id, input.get("status"));
    return Result.ok(null);
  }
}
