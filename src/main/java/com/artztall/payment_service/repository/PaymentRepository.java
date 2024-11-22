package com.artztall.payment_service.repository;

import com.artztall.payment_service.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    Payment findByStripPaymentIntendId(String stripPaymentIntendId);
}
