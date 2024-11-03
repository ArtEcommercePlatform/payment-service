package com.artztall.payment_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;



@Data
@Schema(description = "Payment request information")
public class PaymentRequestDTO {

    @Schema(description = "Unique identifier for the order"
            )
    @NotBlank(message = "Order ID is required")
    private String orderId;

    @Schema(description = "Unique identifier for the user",
            example = "user_456"
            )
    @NotBlank(message = "User ID is required")
    private String userId;

    @Schema(description = "Payment amount in smallest currency unit (e.g., cents)",
            example = "1999")
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Long amount;

    @Schema(description = "Three-letter ISO currency code",
            example = "USD")
    @NotBlank(message = "Currency is required")
    private String currency;

    @Schema(description = "Identifier for the payment method",
            example = "pm_card_visa")
    @NotBlank(message = "Payment method ID is required")
    private String paymentMethodId;
}