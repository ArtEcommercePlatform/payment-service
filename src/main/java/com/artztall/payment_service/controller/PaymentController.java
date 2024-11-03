package com.artztall.payment_service.controller;

import com.artztall.payment_service.dto.PaymentRequestDTO;
import com.artztall.payment_service.dto.PaymentResponseDTO;
import com.artztall.payment_service.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Operations", description = "Endpoints for payment processing")
public class PaymentController {
    private final PaymentService paymentService;

    @Operation(summary = "Create a new payment",
            description = "Initiates a new payment transaction with the provided payment details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment created successfully",
                    content = @Content(schema = @Schema(implementation = PaymentResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<PaymentResponseDTO> createPayment(
            @RequestBody PaymentRequestDTO paymentRequestDTO) {
        return ResponseEntity.ok(paymentService.createPayment(paymentRequestDTO));
    }

    @Operation(summary = "Confirm a payment",
            description = "Confirms a previously initiated payment using the payment intent ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment confirmed successfully",
                    content = @Content(schema = @Schema(implementation = PaymentResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Payment intent not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("confirm/{paymentIntentId}")
    public ResponseEntity<PaymentResponseDTO> confirmPayment(
            @Parameter(description = "Payment intent identifier")
            @PathVariable String paymentIntentId) {
        return ResponseEntity.ok(paymentService.confirmPayment(paymentIntentId));
    }

    @Operation(summary = "Refund a payment",
            description = "Initiates a refund for a completed payment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Refund processed successfully",
                    content = @Content(schema = @Schema(implementation = PaymentResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Payment intent not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("payment/{paymentIntentId}")
    public ResponseEntity<PaymentResponseDTO> refundPayment(
            @Parameter(description = "Payment intent identifier")
            @PathVariable String paymentIntentId) {
        return ResponseEntity.ok(paymentService.refundPayment(paymentIntentId));
    }

    @Operation(summary = "Get payment status",
            description = "Retrieves the current status of a payment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment status retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PaymentResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Payment intent not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("status/{paymentIntentId}")
    public ResponseEntity<PaymentResponseDTO> getPaymentStatus(
            @Parameter(description = "Payment intent identifier")
            @PathVariable String paymentIntentId) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(paymentIntentId));
    }
}