package com.artztall.payment_service.service;

import com.artztall.payment_service.dto.PaymentRequestDTO;
import com.artztall.payment_service.dto.PaymentResponseDTO;
import com.artztall.payment_service.model.Payment;
import com.artztall.payment_service.model.PaymentStatus;
import com.artztall.payment_service.repository.PaymentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService{
    private final PaymentRepository paymentRepository;

    @Override
    public PaymentResponseDTO createPayment(PaymentRequestDTO paymentRequest){
        try{
            PaymentIntentCreateParams createParams = PaymentIntentCreateParams.builder()
                    .setAmount(paymentRequest.getAmount())
                    .setCurrency(paymentRequest.getCurrency())
                    .setPaymentMethod(paymentRequest.getPaymentMethodId())
                    .setConfirmationMethod(PaymentIntentCreateParams.ConfirmationMethod.MANUAL)
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(createParams);

            Payment payment = new Payment();

            payment.setOrderId(paymentRequest.getOrderId());
            payment.setUserid(paymentRequest.getUserId());
            payment.setAmount(paymentRequest.getAmount());
            payment.setCurrency(paymentRequest.getCurrency());
            payment.setStripPaymentIntendId(paymentIntent.getId());
            payment.setPaymentStatus(PaymentStatus.PENDING);
            payment.setCreatedAt(LocalDateTime.now());
            payment.setUpdatedAt(LocalDateTime.now());

            PaymentResponseDTO response = new PaymentResponseDTO();
            response.setPaymentId(payment.getId());
            response.setClientSecret(paymentIntent.getClientSecret());
            response.setStatus(PaymentStatus.PENDING);
            return response;

        } catch (StripeException e) {
            PaymentResponseDTO response = new PaymentResponseDTO();
            response.setStatus(PaymentStatus.FAILED);
            response.setMessage(e.getMessage());
            return response;
        }
    }

    @Override
    public PaymentResponseDTO confirmPayment(String paymentIntentId){
        try{
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            paymentIntent.confirm();

           Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntentId);
           payment.setPaymentStatus(PaymentStatus.COMPLETED);
           payment.setUpdatedAt(LocalDateTime.now());
           paymentRepository.save(payment);

           PaymentResponseDTO response = new PaymentResponseDTO();

           response.setPaymentId(payment.getId());
           response.setStatus(PaymentStatus.COMPLETED);
           return response;


        } catch (StripeException e) {
            PaymentResponseDTO response = new PaymentResponseDTO();
            response.setStatus(PaymentStatus.FAILED);
            response.setMessage(e.getMessage());
            return response;
        }
    }


    @Override
    public PaymentResponseDTO refundPayment(String paymentId) {

        return null;
    }

    @Override
    public PaymentResponseDTO getPaymentStatus(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        PaymentResponseDTO response = new PaymentResponseDTO();

        if (payment != null) {
            response.setPaymentId(payment.getId());
            response.setStatus(payment.getPaymentStatus());
        } else {
            response.setStatus(PaymentStatus.FAILED);
            response.setMessage("Payment not found");
        }

        return response;
    }
}
