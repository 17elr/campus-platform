package com.campus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.entity.User;
import com.campus.mapper.UserMapper;
import com.campus.security.Passwords;
import com.campus.service.UserService;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

  private final UserMapper mapper;

  public UserServiceImpl(UserMapper mapper) {
    this.mapper = mapper;
  }

  public User login(String name, String password) {
    User u = mapper.selectOne(new QueryWrapper<User>().eq("username", name));
    if (u == null || !Integer.valueOf(1).equals(u.getStatus())) return null;
    boolean hashed = u.getPassword().startsWith("pbkdf2$");
    boolean valid = hashed
      ? Passwords.matches(password, u.getPassword())
      : java.security.MessageDigest.isEqual(
          password.getBytes(java.nio.charset.StandardCharsets.UTF_8),
          u.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
    if (!valid) return null;
    if (!hashed) {
      u.setPassword(Passwords.hash(password));
      mapper.updateById(u);
    }
    u.setPassword(null);
    return u;
  }

  public User register(User input) {
    if (
      mapper.selectCount(
        new QueryWrapper<User>().eq("username", input.getUsername())
      ) > 0
    ) throw new IllegalArgumentException("用户名已被使用");
    User u = new User();
    u.setUsername(input.getUsername());
    u.setNickname(input.getNickname());
    u.setPassword(Passwords.hash(input.getPassword()));
    u.setRole("STUDENT");
    u.setStatus(1);
    mapper.insert(u);
    u.setPassword(null);
    return u;
  }

  public List<User> list() {
    List<User> users = mapper.selectList(null);
    users.forEach(u -> u.setPassword(null));
    return users;
  }
}
