package com.artztall.payment_service.dto;

import com.artztall.payment_service.model.OrderStatus;
import com.artztall.payment_service.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponseDTO {
    private String id;
    private String userId;
    private OrderItemResponseDTO item;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private String shippingAddress;
    private String specialInstructions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}