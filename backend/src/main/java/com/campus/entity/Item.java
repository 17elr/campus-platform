package com.campus.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("item")
public class Item {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String title, description, type, category, location, contact, imageUrl, status;

  @TableField(exist = false)
  private java.util.List<String> promotions = java.util.List.of();

  private Long userId;
  private LocalDateTime createdAt;
}
