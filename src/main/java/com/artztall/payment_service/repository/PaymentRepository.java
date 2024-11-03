package com.artztall.payment_service.repository;

import com.artztall.payment_service.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    List<Payment> findByUserId(String userId);
    Payment findByOrderId(String orderId);

    Payment findByStripePaymentIntentId(String paymentIntentId);
}
