package com.artztall.payment_service.service;

import com.artztall.payment_service.dto.PaymentRequestDTO;
import com.artztall.payment_service.dto.PaymentResponseDTO;

public interface PaymentService {
    PaymentResponseDTO createPayment(PaymentRequestDTO paymentRequest);
    PaymentResponseDTO confirmPayment(String paymentIntentId);
    PaymentResponseDTO refundPayment(String paymentId);
    PaymentResponseDTO getPaymentStatus(String paymentId);
}
