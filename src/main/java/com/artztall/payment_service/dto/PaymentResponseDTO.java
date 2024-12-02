package com.artztall.payment_service.dto;

import com.artztall.payment_service.model.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Payment response information")
public class PaymentResponseDTO {

    private String paymentId;

    private String clientSecret;

    private PaymentStatus paymentStatus;

    private String message;

    private LocalDateTime expiresAt;
}