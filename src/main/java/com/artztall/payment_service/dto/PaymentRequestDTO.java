package com.artztall.payment_service.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequestDTO {
    private String orderId;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethodId;
}
