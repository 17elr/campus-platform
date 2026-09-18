package com.campus.controller;
import com.campus.common.Result;
import com.campus.mapper.UserMapper;
import com.campus.security.Tokens;
import com.campus.service.PromotionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*; import java.util.Map;

@RestController @RequestMapping("/api")
public class PromotionController {
 private final PromotionService service; private final com.campus.service.PlatformSettingService settings; private final Tokens tokens; private final UserMapper users;
 public PromotionController(PromotionService s,com.campus.service.PlatformSettingService ps,Tokens t,UserMapper u){service=s;settings=ps;tokens=t;users=u;}
 private long me(String h){if(h==null||!h.startsWith("Bearer "))throw new IllegalArgumentException("请先登录");return tokens.verify(h.substring(7));}
 private void admin(String h){if(!"ADMIN".equals(users.selectById(me(h)).getRole()))throw new IllegalArgumentException("无管理员权限");}
 public record Create(@NotNull @Positive Long itemId,@NotBlank String promotionType){}
 @GetMapping("/promotions/plans") public Result<?> plans(){return Result.ok(service.plans());}
 @GetMapping("/admin/settings") public Result<?> settings(@RequestHeader("Authorization") String h){admin(h);return Result.ok(settings.all());}
 @PutMapping("/admin/settings") public Result<?> saveSettings(@RequestHeader("Authorization") String h,@RequestBody Map<String,String> body){long uid=me(h);admin(h);settings.save(body,uid);return Result.ok(settings.all());}
 @PostMapping("/promotions/orders") public Result<?> create(@RequestHeader(value="Authorization",required=false) String h,@Valid @RequestBody Create body){return Result.ok(service.create(me(h),body.itemId(),body.promotionType()));}
 @PostMapping("/promotions/orders/{id}/pay") public Result<?> pay(@RequestHeader(value="Authorization",required=false) String h,@PathVariable long id){return Result.ok(service.pay(me(h),id));}
 @PostMapping("/promotions/orders/{id}/cancel") public Result<?> cancel(@RequestHeader(value="Authorization",required=false) String h,@PathVariable long id){service.cancel(me(h),id);return Result.ok(null);}
 @GetMapping("/promotions/orders") public Result<?> list(@RequestHeader(value="Authorization",required=false) String h,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size){return Result.ok(service.list(me(h),page,size));}
 @GetMapping("/admin/promotions/orders") public Result<?> all(@RequestHeader(value="Authorization",required=false) String h,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size){admin(h);return Result.ok(service.list(null,page,size));}
 @GetMapping("/admin/promotions/stats") public Result<?> stats(@RequestHeader(value="Authorization",required=false) String h){admin(h);return Result.ok(service.stats());}
}

