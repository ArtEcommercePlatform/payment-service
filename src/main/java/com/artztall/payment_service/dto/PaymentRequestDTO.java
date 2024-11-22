package com.artztall.payment_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
    @Schema(description = "Three-letter ISO currency code",
            example = "USD")
    @NotBlank(message = "Currency is required")
    private String currency;

    @Schema(description = "Identifier for the payment method",
            example = "pm_card_visa")
    @NotBlank(message = "Payment method ID is required")
    private String paymentMethodId;
}