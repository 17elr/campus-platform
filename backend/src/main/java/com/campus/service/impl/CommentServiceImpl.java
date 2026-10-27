package com.campus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.dto.CommentDTO;
import com.campus.entity.*;
import com.campus.mapper.*;
import com.campus.security.Tokens;
import com.campus.service.CommentService;
import com.campus.vo.CommentVO;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentServiceImpl implements CommentService {

  private final CommentMapper comments;
  private final UserMapper users;
  private final ItemMapper items;
  private final Tokens tokens;

  public CommentServiceImpl(
    CommentMapper c,
    UserMapper u,
    ItemMapper i,
    Tokens t
  ) {
    comments = c;
    users = u;
    items = i;
    tokens = t;
  }

  private void checkItem(Long id) {
    Item item = items.selectById(id);
    if (
      item == null || !"APPROVED".equals(item.getStatus())
    ) throw new IllegalArgumentException("物品不存在或尚未公开");
  }

  private User authenticate(String header) {
    if (
      header == null || !header.startsWith("Bearer ")
    ) throw new IllegalArgumentException("请先登录");
    User u = users.selectById(tokens.verify(header.substring(7)));
    if (
      u == null || !Integer.valueOf(1).equals(u.getStatus())
    ) throw new IllegalArgumentException("账号不可用，请重新登录");
    return u;
  }

  public List<CommentVO> list(Long itemId) {
    checkItem(itemId);
    return comments
      .selectList(
        new QueryWrapper<Comment>()
          .eq("item_id", itemId)
          .orderByAsc("created_at", "id")
      )
      .stream()
      .map(c -> {
        User u = users.selectById(c.getUserId());
        boolean deleted = Integer.valueOf(1).equals(c.getDeleted());
        return new CommentVO(
          c.getId(),
          c.getUserId(),
          u == null ? "已注销用户" : u.getNickname(),
          c.getParentId(),
          deleted ? "该留言已删除" : c.getContent(),
          deleted,
          c.getCreatedAt()
        );
      })
      .toList();
  }

  @Transactional
  public Long create(Long itemId, CommentDTO input, String header) {
    User u = authenticate(header);
    checkItem(itemId);
    if (
      input.content() == null ||
      input.content().isBlank() ||
      input.content().length() > 500
    ) throw new IllegalArgumentException("请输入1-500字留言");
    if (input.parentId() != null) {
      Comment parent = comments.selectById(input.parentId());
      if (
        parent == null ||
        !itemId.equals(parent.getItemId()) ||
        Integer.valueOf(1).equals(parent.getDeleted())
      ) throw new IllegalArgumentException("回复目标不存在或已删除");
    }
    Comment c = new Comment();
    c.setItemId(itemId);
    c.setUserId(u.getId());
    c.setParentId(input.parentId());
    c.setContent(input.content().strip());
    c.setDeleted(0);
    c.setCreatedAt(LocalDateTime.now());
    comments.insert(c);
    return c.getId();
  }

  @Transactional
  public void delete(Long itemId, Long id, String header) {
    User u = authenticate(header);
    Comment c = comments.selectById(id);
    if (
      c == null || !itemId.equals(c.getItemId())
    ) throw new IllegalArgumentException("留言不存在");
    if (
      !u.getId().equals(c.getUserId()) && !"ADMIN".equals(u.getRole())
    ) throw new IllegalArgumentException("只能删除自己的留言");
    c.setDeleted(1);
    c.setContent("");
    comments.updateById(c);
  }
}
