package com.artztall.payment_service.dto;

import com.artztall.payment_service.model.PaymentStatus;

public class PaymentResponseDTO {
    private String paymentId;
    private String clientSecret;
    private PaymentStatus status;
    private String message;
}
