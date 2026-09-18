package com.campus.controller;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper; import com.campus.common.Result; import com.campus.entity.Item; import com.campus.mapper.ItemMapper; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/items") public class ItemController { private final com.campus.service.PromotionService promotions; private final ItemMapper items; private final com.campus.security.Tokens tokens; private final com.campus.mapper.UserMapper users; public ItemController(ItemMapper i,com.campus.security.Tokens t,com.campus.mapper.UserMapper u,com.campus.service.PromotionService p){promotions=p;items=i;tokens=t;users=u;}
 @GetMapping public Result<?> list(@RequestParam(required=false) String keyword,@RequestParam(required=false) String type,@RequestParam(required=false) String location,@RequestParam(required=false) String category,@RequestParam(defaultValue="hall") String scene,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="12") int size){
  var paging=new com.campus.common.Paging(page,size);var w=new QueryWrapper<Item>().eq("status","APPROVED");
  if(keyword!=null&&!keyword.isBlank())w.and(x->x.like("title",keyword).or().like("description",keyword).or().like("category",keyword));
  if(type!=null&&!type.isBlank())w.eq("type",type);if(location!=null&&!location.isBlank())w.eq("location",location);if(category!=null&&!category.isBlank())w.eq("category",category);
  long total=items.selectCount(w);w.orderByDesc(com.campus.service.PromotionService.ranking(scene,category!=null&&!category.isBlank())).orderByDesc("created_at","id").last(paging.limit());
  var records=items.selectList(w);promotions.decorate(records);var counts=new HashMap<String,Long>();for(String t:List.of("LOST","FOUND","SECOND_HAND"))counts.put(t,items.selectCount(new QueryWrapper<Item>().eq("type",t).eq("status","APPROVED")));
  return Result.ok(Map.of("records",records,"total",total,"page",paging.page(),"size",paging.size(),"counts",counts));
 }
 @GetMapping("/counts") public Result<?> counts(){ Map<String,Long> out=new HashMap<>(); for(String type: List.of("LOST","FOUND","SECOND_HAND")) out.put(type, (long)items.selectCount(new QueryWrapper<Item>().eq("type",type).eq("status","APPROVED"))); return Result.ok(out); } @GetMapping("/{id}") public Result<?> detail(@PathVariable Long id){Item item=items.selectById(id);if(item==null)return Result.fail("信息不存在");promotions.decorate(List.of(item));return Result.ok(item);}
 @PostMapping public Result<?> create(@RequestBody Item input,@RequestHeader(value="Authorization",required=false) String authorization){
  if(authorization==null||!authorization.startsWith("Bearer "))return Result.fail("请先登录");
  Long uid=tokens.verify(authorization.substring(7));
  if(input.getCategory()==null||input.getCategory().isBlank())input.setCategory("其他");
  if(input.getImageUrl()!=null&&!input.getImageUrl().isBlank()&&!input.getImageUrl().matches("/api/images/[a-f0-9-]{36}\\.png"))return Result.fail("图片地址无效");
  if(!Set.of("LOST","FOUND","SECOND_HAND").contains(input.getType()==null?"":input.getType()))return Result.fail("请选择有效的信息类型");
  String[] fields={input.getTitle(),input.getDescription(),input.getCategory(),input.getLocation(),input.getContact()};
  int[] limits={100,5000,50,100,100};
  for(int n=0;n<fields.length;n++)if(fields[n]==null||fields[n].isBlank()||fields[n].length()>limits[n])return Result.fail("请完整填写信息，且不要超过字段长度限制");
  Item i=new Item();i.setTitle(input.getTitle().trim());i.setDescription(input.getDescription().trim());i.setType(input.getType());i.setCategory(input.getCategory().trim());i.setLocation(input.getLocation().trim());i.setContact(input.getContact().trim());
  i.setImageUrl(input.getImageUrl());i.setUserId(uid);i.setStatus("APPROVED");i.setCreatedAt(java.time.LocalDateTime.now());items.insert(i);return Result.ok(i);
 }
 @PutMapping("/{id}/approve") public Result<?> approve(@PathVariable Long id,@RequestHeader(value="Authorization",required=false) String auth){if(auth==null||!auth.startsWith("Bearer "))return Result.fail("无权限");com.campus.entity.User admin=users.selectById(tokens.verify(auth.substring(7)));if(admin==null||!"ADMIN".equals(admin.getRole()))return Result.fail("无管理员权限");Item i=items.selectById(id);if(i==null)return Result.fail("信息不存在");i.setStatus("APPROVED");items.updateById(i);return Result.ok(i);}
 private Long owner(String auth){if(!auth.startsWith("Bearer "))throw new IllegalArgumentException("请先登录");return tokens.verify(auth.substring(7));}
 @GetMapping("/mine") public Result<?> mine(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size,@RequestHeader("Authorization") String auth){
 var paging=new com.campus.common.Paging(page,size);var w=new QueryWrapper<Item>().eq("user_id",owner(auth)).ne("status","DELETED");long total=items.selectCount(w);w.orderByDesc("created_at","id").last(paging.limit());var records=items.selectList(w);promotions.decorate(records);return Result.ok(Map.of("records",records,"total",total,"page",paging.page(),"size",paging.size()));}
 @PutMapping("/{id}/withdraw") public Result<?> withdraw(@PathVariable Long id,@RequestHeader("Authorization") String auth){Long uid=owner(auth);Item item=items.selectById(id);if(item==null||!uid.equals(item.getUserId()))return Result.fail("只能管理自己的信息");if(!"APPROVED".equals(item.getStatus()))return Result.fail("当前信息不可下架");item.setStatus("WITHDRAWN");items.updateById(item);return Result.ok(null);}
 @DeleteMapping("/{id}") public Result<?> delete(@PathVariable Long id,@RequestHeader("Authorization") String auth){Long uid=owner(auth);Item item=items.selectById(id);if(item==null||!uid.equals(item.getUserId()))return Result.fail("只能删除自己的信息");item.setStatus("DELETED");items.updateById(item);return Result.ok(null);}
}








