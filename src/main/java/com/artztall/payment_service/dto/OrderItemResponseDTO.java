package com.artztall.payment_service.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemResponseDTO {
    private String productId;
    private String productName;
    private String artistId;
    private int quantity;
    private BigDecimal price;
    private BigDecimal subtotal;
    private String imageUrl;
    private String medium;
    private String style;
}