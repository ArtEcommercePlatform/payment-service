package com.artztall.payment_service.model;


import lombok.NoArgsConstructor;

@NoArgsConstructor
public enum PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    EXPIRED, REFUNDED
}
