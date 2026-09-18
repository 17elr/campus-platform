package com.campus.controller;
import com.campus.common.Result; import com.campus.entity.Item; import com.campus.entity.TradeOrder; import com.campus.entity.User; import com.campus.mapper.*; import com.campus.security.Tokens; import org.springframework.web.bind.annotation.*; import java.util.*; import java.math.BigDecimal;
@RestController @RequestMapping("/api/admin") public class AdminController { private final UserMapper u; private final ItemMapper i; private final TradeOrderMapper orders; private final Tokens tokens; private BigDecimal commissionRate=new BigDecimal("0.03"); public AdminController(UserMapper u,ItemMapper i,TradeOrderMapper orders,Tokens tokens){this.u=u;this.i=i;this.orders=orders;this.tokens=tokens;} private void admin(String h){if(h==null||!h.startsWith("Bearer "))throw new IllegalArgumentException("请先登录");User x=u.selectById(tokens.verify(h.substring(7)));if(x==null||!Integer.valueOf(1).equals(x.getStatus())||!"ADMIN".equals(x.getRole()))throw new IllegalArgumentException("无管理员权限");} @GetMapping("/stats") public Result<?> stats(@RequestHeader("Authorization") String h){admin(h);return Result.ok(Map.of("users",u.selectCount(null),"items",i.selectCount(null),"pending",i.selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Item>().eq("status","PENDING")),"approved",i.selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Item>().eq("status","APPROVED"))));} @GetMapping("/trade/stats") public Result<?> tradeStats(@RequestHeader("Authorization") String h){admin(h); var all=orders.selectList(null); var paid=all.stream().filter(x->"PAID".equals(x.getStatus())).toList(); BigDecimal gross=paid.stream().map(x->x.getAmount()==null?BigDecimal.ZERO:x.getAmount()).reduce(BigDecimal.ZERO,BigDecimal::add); BigDecimal fees=paid.stream().map(x->x.getCommission()==null?BigDecimal.ZERO:x.getCommission()).reduce(BigDecimal.ZERO,BigDecimal::add); var trend=new ArrayList<Map<String,Object>>(); for(int d=6;d>=0;d--){ java.time.LocalDate day=java.time.LocalDate.now().minusDays(d); var rows=paid.stream().filter(x->x.getPaidAt()!=null&&x.getPaidAt().toLocalDate().equals(day)).toList(); BigDecimal amount=rows.stream().map(x->x.getAmount()==null?BigDecimal.ZERO:x.getAmount()).reduce(BigDecimal.ZERO,BigDecimal::add); BigDecimal fee=rows.stream().map(x->x.getCommission()==null?BigDecimal.ZERO:x.getCommission()).reduce(BigDecimal.ZERO,BigDecimal::add); trend.add(Map.of("date",day.toString(),"orders",rows.size(),"gross",amount,"commission",fee)); } return Result.ok(Map.of("orders",paid.size(),"gross",gross,"commission",fees,"visitors",u.selectCount(null),"commissionRate",commissionRate,"trend",trend)); } @GetMapping("/trade/orders") public Result<?> tradeOrders(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size,@RequestHeader("Authorization") String h){admin(h); long total=orders.selectCount(null); var list=orders.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TradeOrder>().orderByDesc("created_at").last("LIMIT "+Math.min(size,100)+" OFFSET "+Math.max(0,(page-1)*size))); return Result.ok(Map.of("records",list,"total",total,"page",page,"size",size)); } @PutMapping("/trade/commission") public Result<?> setCommission(@RequestBody Map<String,BigDecimal> body,@RequestHeader("Authorization") String h){admin(h); BigDecimal rate=body.get("rate"); if(rate==null||rate.compareTo(BigDecimal.ZERO)<0||rate.compareTo(BigDecimal.ONE)>0)return Result.fail("佣金比例必须在0%到100%之间"); commissionRate=rate; return Result.ok(Map.of("rate",rate)); } @GetMapping("/users") public Result<?> users(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size,@RequestHeader("Authorization") String h){admin(h); long total=u.selectCount(null); var list=u.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>().orderByDesc("created_at").last("LIMIT "+Math.min(size,100)+" OFFSET "+Math.max(0,(page-1)*size)));list.forEach(user->user.setPassword(null));return Result.ok(Map.of("records",list,"total",total,"page",page,"size",size));} @GetMapping("/items") public Result<?> items(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size,@RequestHeader("Authorization") String h){admin(h); long total=i.selectCount(null); var list=i.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Item>().orderByDesc("created_at").last("LIMIT "+Math.min(size,100)+" OFFSET "+Math.max(0,(page-1)*size)));return Result.ok(Map.of("records",list,"total",total,"page",page,"size",size));} @GetMapping("/items/pending") public Result<?> pending(@RequestHeader("Authorization") String h){admin(h);return Result.ok(i.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Item>().eq("status","PENDING").orderByDesc("created_at")));} 
 @PutMapping("/users/{id}/status")
 public Result<?> userStatus(@PathVariable Long id,@RequestBody Map<String,Integer> body,@RequestHeader("Authorization") String h){
  admin(h);User user=u.selectById(id);
  if(user==null||Integer.valueOf(-1).equals(user.getStatus()))return Result.fail("用户不存在");
  if("ADMIN".equals(user.getRole()))return Result.fail("不能操作管理员账号");
  Integer next=body.get("status");
  if(next==null||(next!=0&&next!=1))return Result.fail("状态无效");
  user.setStatus(next);u.updateById(user);return Result.ok(null);
 }
 @DeleteMapping("/users/{id}")
 public Result<?> deleteUser(@PathVariable Long id,@RequestHeader("Authorization") String h){
  admin(h);User user=u.selectById(id);
  if(user==null)return Result.fail("用户不存在");
  if("ADMIN".equals(user.getRole()))return Result.fail("不能删除管理员账号");
  user.setStatus(-1);u.updateById(user);return Result.ok(null);
 }
 @PutMapping("/items/{id}/status")
 public Result<?> status(@PathVariable Long id,@RequestBody Map<String,String> body,@RequestHeader("Authorization") String h){
  admin(h);
  String status=body.get("status");
  if(!"APPROVED".equals(status)&&!"HIDDEN".equals(status))return Result.fail("状态无效");
  Item item=i.selectById(id);
  if(item==null)return Result.fail("信息不存在");
  item.setStatus(status);i.updateById(item);return Result.ok(item);
 }
}





