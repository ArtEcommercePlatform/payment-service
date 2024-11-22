package com.artztall.payment_service.model;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Document(collection = "payments")
@Builder
public class Payment {
    @Id
    private String id;
    private String orderId;
    private String userId;
    private Long amount;
    private String currency;
    private String stripPaymentIntendId;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
