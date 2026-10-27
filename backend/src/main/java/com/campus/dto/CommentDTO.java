package com.campus.dto;

import jakarta.validation.constraints.*;

public record CommentDTO(
  @NotBlank @Size(max = 500, message = "留言最多500字") String content,
  Long parentId
) {}
