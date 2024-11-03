package com.artztall.payment_service.dto;

import com.artztall.payment_service.model.PaymentStatus;
import lombok.Data;

@Data
public class PaymentResponseDTO {
    private String paymentId;
    private String clientSecret;
    private PaymentStatus status;
    private String message;
}
