package com.campus.controller;
import com.campus.common.Result; import com.campus.dto.*; import com.campus.entity.User; import com.campus.mapper.UserMapper; import com.campus.service.UserService; import com.campus.security.Tokens; import com.campus.vo.LoginVO;
import jakarta.validation.Valid; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth") public class AuthController {
 private final UserService service;private final Tokens tokens;private final UserMapper users;
 public AuthController(UserService service,Tokens tokens,UserMapper users){this.service=service;this.tokens=tokens;this.users=users;}
 @PostMapping("/register") public Result<?> register(@Valid @RequestBody RegisterDTO q){User u=new User();u.setUsername(q.username());u.setPassword(q.password());u.setNickname(q.nickname());return Result.ok(service.register(u));}
 @PostMapping("/login") public Result<?> login(@Valid @RequestBody LoginDTO q){User u=service.login(q.username(),q.password());if(u==null)return new Result<>(401,"账号或密码错误，或账号已被禁用",null);return Result.ok(new LoginVO(tokens.issue(u.getId()),u));}
 @GetMapping("/me") public Result<?> me(@RequestHeader("Authorization") String authorization){if(!authorization.startsWith("Bearer "))throw new IllegalArgumentException("请先登录");User u=users.selectById(tokens.verify(authorization.substring(7)));if(u==null||!Integer.valueOf(1).equals(u.getStatus()))return new Result<>(401,"登录失效",null);u.setPassword(null);return Result.ok(u);}
 @PostMapping("/logout") public Result<?> logout(@RequestHeader(value="Authorization",defaultValue="") String header){if(header.startsWith("Bearer "))tokens.revoke(header.substring(7));return Result.ok(null);}
}
