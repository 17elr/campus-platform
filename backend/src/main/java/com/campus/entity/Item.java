package com.campus.entity; import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.time.LocalDateTime;
@Data @TableName("item") public class Item { @TableId(type=IdType.AUTO) private Long id; private String title,description,type,category,location,contact,imageUrl,status; private Long userId; private LocalDateTime createdAt; }
