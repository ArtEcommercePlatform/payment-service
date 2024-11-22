package com.artztall.payment_service.dto;

import com.artztall.payment_service.model.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Payment response information")
public class PaymentResponseDTO {

    @Schema(description = "Unique identifier for the payment",
            example = "pi_3Nk9Xy2eZvKYlo2C1KOYGKqB")
    private String paymentId;

    @Schema(description = "Client secret for payment confirmation",
            example = "pi_3Nk9Xy2eZvKYlo2C1KOYGKqB_secret_abcdef")
    private String clientSecret;

    @Schema(description = "Current status of the payment",
            example = "COMPLETED")
    private PaymentStatus status;

    @Schema(description = "Additional information about the payment status",
            example = "Payment processed successfully")
    private String message;
}