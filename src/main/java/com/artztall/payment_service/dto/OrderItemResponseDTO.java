package com.artztall.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
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