package com.campus.dto;

import jakarta.validation.constraints.*;

public record RegisterDTO(
  @NotBlank
  @Pattern(
    regexp = "[A-Za-z0-9_]{3,30}",
    message = "用户名需为3-30位字母、数字或下划线"
  )
  String username,
  @NotBlank
  @Size(min = 8, max = 72, message = "密码长度应为8-72位")
  String password,
  @NotBlank @Size(max = 50) String nickname
) {}
