package com.campus.dto;
import jakarta.validation.constraints.*;
public record LoginDTO(@NotBlank @Size(max=50) String username,@NotBlank @Size(max=100) String password) {}
