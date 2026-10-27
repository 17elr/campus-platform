package com.campus.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record OrderDTO(
  @NotNull Long productId,
  @NotNull @DecimalMin("0.01") BigDecimal amount,
  @NotBlank String paymentMethod
) {}
