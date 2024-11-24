package com.artztall.payment_service.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductResponseDTO {
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private String artistId;
    private String category;
    private List<String> tags;
    private String imageUrl;
    private Integer stockQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isAvailable;
    private String medium;
    private String style;
}
