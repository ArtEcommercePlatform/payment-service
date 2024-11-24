package com.artztall.payment_service.repository;

import com.artztall.payment_service.model.Payment;
import com.artztall.payment_service.model.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    Payment findByStripPaymentIntendId(String stripPaymentIntendId);

    List<Payment> findByPaymentStatusAndExpiresAtBefore(PaymentStatus paymentStatus, LocalDateTime now);
}
