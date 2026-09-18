package com.campus.entity;
import lombok.Data;import com.baomidou.mybatisplus.annotation.*;import java.time.LocalDateTime;
@Data @TableName("item_comment") public class Comment {
 @TableId(type=IdType.AUTO) private Long id;
 private Long itemId,userId,parentId;private String content;private Integer deleted;private LocalDateTime createdAt;
}
