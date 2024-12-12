package com.artztall.payment_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;



@Data
@Builder
@Schema(description = "Payment request information")
public class PaymentRequestDTO {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotBlank(message = "Payment method ID is required")
    private String paymentMethodId;
}