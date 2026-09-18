package com.campus.vo;
import java.time.LocalDateTime;
public record CommentVO(Long id,Long userId,String nickname,Long parentId,String content,boolean deleted,LocalDateTime createdAt){}
