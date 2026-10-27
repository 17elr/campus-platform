package com.campus.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("trade_order")
public class TradeOrder {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String orderNo, status, paymentMethod;
  private Long productId, buyerId, sellerId;
  private BigDecimal amount, commission, sellerAmount;
  private Long commissionRuleId;
  private LocalDateTime createdAt, paidAt;
}
