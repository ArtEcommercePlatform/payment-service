package com.artztall.payment_service.service;

import com.artztall.payment_service.dto.OrderResponseDTO;
import com.artztall.payment_service.dto.PaymentRequestDTO;
import com.artztall.payment_service.dto.PaymentResponseDTO;
import com.artztall.payment_service.exception.PaymentNotFoundException;
import com.artztall.payment_service.exception.PaymentProcessingException;
import com.artztall.payment_service.model.Payment;
import com.artztall.payment_service.model.PaymentStatus;
import com.artztall.payment_service.repository.PaymentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.net.RequestOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderClientService orderClientService;

    @Override
    public PaymentResponseDTO createPayment(PaymentRequestDTO paymentRequest) {
        try {
            log.info("Processing payment for order: {}", paymentRequest.getOrderId());
            OrderResponseDTO orderResponseDTO = orderClientService.getOrder(paymentRequest.getOrderId());

            validatePaymentRequest(paymentRequest);

            // Create parameters for Stripe
            PaymentIntentCreateParams createParams = PaymentIntentCreateParams.builder()
                    .setAmount(orderResponseDTO.getTotalAmount().longValue())
                    .setCurrency(paymentRequest.getCurrency())
                    .setPaymentMethod(paymentRequest.getPaymentMethodId())
                    .setConfirmationMethod(PaymentIntentCreateParams.ConfirmationMethod.MANUAL)
                    .build();

            // Create RequestOptions with idempotency key
            RequestOptions requestOptions = RequestOptions.builder()
                    .setIdempotencyKey(paymentRequest.getOrderId())
                    .build();

            // Create PaymentIntent with proper RequestOptions
            PaymentIntent paymentIntent = PaymentIntent.create(createParams, requestOptions);

            Payment payment = Payment.builder()
                    .orderId(paymentRequest.getOrderId())
                    .userId(paymentRequest.getUserId())
                    .amount(orderResponseDTO.getTotalAmount().longValue())
                    .currency(paymentRequest.getCurrency())
                    .stripPaymentIntendId(paymentIntent.getId())
                    .paymentStatus(PaymentStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            payment = paymentRepository.save(payment);

            log.info("Payment created successfully for order: {}", payment.getOrderId());

            return PaymentResponseDTO.builder()
                    .paymentId(payment.getId())
                    .clientSecret(paymentIntent.getClientSecret())
                    .status(PaymentStatus.PENDING)
                    .message("Payment created successfully")
                    .build();

        } catch (StripeException e) {
            log.error("Stripe payment processing failed for order: {}", paymentRequest.getOrderId(), e);
            return PaymentResponseDTO.builder()
                    .status(PaymentStatus.FAILED)
                    .message(e.getMessage())
                    .build();
        }
    }

    @Override
    public PaymentResponseDTO confirmPayment(String paymentIntentId) {
        try {
            log.info("Confirming payment for paymentIntentId: {}", paymentIntentId);

            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            PaymentIntentConfirmParams confirmParams = PaymentIntentConfirmParams.builder().build();
            paymentIntent.confirm(confirmParams);

            Payment payment = paymentRepository.findByStripPaymentIntendId(paymentIntentId);
            if (payment == null) {
                throw new PaymentNotFoundException("Payment not found for intent: " + paymentIntentId);
            }

            payment.setPaymentStatus(PaymentStatus.COMPLETED);
            payment.setUpdatedAt(LocalDateTime.now());
            payment = paymentRepository.save(payment);

            return PaymentResponseDTO.builder()
                    .paymentId(payment.getId())
                    .status(PaymentStatus.COMPLETED)
                    .message("Payment confirmed successfully")
                    .build();

        } catch (StripeException e) {
            log.error("Payment confirmation failed for paymentIntentId: {}", paymentIntentId, e);
            return PaymentResponseDTO.builder()
                    .status(PaymentStatus.FAILED)
                    .message("Payment confirmation failed: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public PaymentResponseDTO refundPayment(String paymentId) {
        try {
            log.info("Processing refund for payment: {}", paymentId);

            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));

            if (payment.getPaymentStatus() != PaymentStatus.COMPLETED) {
                throw new PaymentProcessingException("Only completed payments can be refunded");
            }

            RefundCreateParams refundParams = RefundCreateParams.builder()
                    .setPaymentIntent(payment.getStripPaymentIntendId())
                    .build();

            RequestOptions requestOptions = RequestOptions.builder()
                    .setIdempotencyKey("refund_" + payment.getId())
                    .build();

            Refund.create(refundParams, requestOptions);

            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            payment.setUpdatedAt(LocalDateTime.now());
            payment = paymentRepository.save(payment);

            return PaymentResponseDTO.builder()
                    .paymentId(payment.getId())
                    .status(PaymentStatus.REFUNDED)
                    .message("Payment refunded successfully")
                    .build();

        } catch (StripeException e) {
            log.error("Refund failed for payment: {}", paymentId, e);
            return PaymentResponseDTO.builder()
                    .status(PaymentStatus.FAILED)
                    .message("Refund processing failed: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public PaymentResponseDTO getPaymentStatus(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElse(null);

        if (payment == null) {
            return PaymentResponseDTO.builder()
                    .status(PaymentStatus.FAILED)
                    .message("Payment not found")
                    .build();
        }

        return PaymentResponseDTO.builder()
                .paymentId(payment.getId())
                .status(payment.getPaymentStatus())
                .message("Payment status: " + payment.getPaymentStatus())
                .build();
    }

    private void validatePaymentRequest(PaymentRequestDTO request) {
        if (!StringUtils.hasText(request.getCurrency())) {
            throw new IllegalArgumentException("Currency is required");
        }
        if (!StringUtils.hasText(request.getPaymentMethodId())) {
            throw new IllegalArgumentException("Payment method is required");
        }
        if (!StringUtils.hasText(request.getOrderId())) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (!StringUtils.hasText(request.getUserId())) {
            throw new IllegalArgumentException("User ID is required");
        }
    }
}