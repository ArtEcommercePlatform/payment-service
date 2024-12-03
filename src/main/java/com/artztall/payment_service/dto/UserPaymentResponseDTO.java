package com.artztall.payment_service.dto;

import com.artztall.payment_service.model.PaymentStatus;
import lombok.Builder;
import lombok.Data;


import java.time.LocalDateTime;

@Data
@Builder
public class UserPaymentResponseDTO {
    private String id;
    private String orderId;
    private String userId;
    private Long amount;
    private String currency;
    private String stripPaymentIntendId;
    private PaymentStatus paymentStatus;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}