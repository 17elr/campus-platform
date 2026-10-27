package com.campus.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user")
public class User {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String username, password, nickname, role, bio, avatarUrl;
  private Integer status;
  private LocalDateTime createdAt;
}
