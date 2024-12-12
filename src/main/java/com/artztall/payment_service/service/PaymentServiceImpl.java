package com.artztall.payment_service.service;

import com.artztall.payment_service.dto.*;
import com.artztall.payment_service.exception.PaymentNotFoundException;
import com.artztall.payment_service.exception.PaymentProcessingException;
import com.artztall.payment_service.model.Payment;
import com.artztall.payment_service.model.PaymentStatus;
import com.artztall.payment_service.model.OrderStatus;
import com.artztall.payment_service.repository.PaymentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.net.RequestOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderClientService orderClientService;
    private final ProductClientService productClientService;
    private final NotificationClientService notificationClientService;

    private static final long PAYMENT_TIMEOUT_MINUTES = 15;

    @Override
    @Transactional
    public PaymentResponseDTO createPayment(PaymentRequestDTO paymentRequest) {
        try {
            log.info("Processing payment for order: {}", paymentRequest.getOrderId());
            OrderResponseDTO orderResponseDTO = orderClientService.getOrder(paymentRequest.getOrderId());

            validatePaymentRequest(paymentRequest);

            // Create parameters for Stripe
            PaymentIntentCreateParams createParams = PaymentIntentCreateParams.builder()
                    .setAmount(orderResponseDTO.getTotalAmount().longValue()*100)
                    .setCurrency(paymentRequest.getCurrency())
                    .setPaymentMethod(paymentRequest.getPaymentMethodId())
                    .setConfirmationMethod(PaymentIntentCreateParams.ConfirmationMethod.AUTOMATIC)
                    .setSetupFutureUsage(PaymentIntentCreateParams.SetupFutureUsage.OFF_SESSION)
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
                    .expiresAt(LocalDateTime.now().plusMinutes(PAYMENT_TIMEOUT_MINUTES))
                    .build();

            payment = paymentRepository.save(payment);

            // Send notification for payment creation
            NotificationSendDTO notification = new NotificationSendDTO();
            notification.setUserId(payment.getUserId());
            notification.setMessage("Payment initiated for your order. Please complete the payment within "
                    + PAYMENT_TIMEOUT_MINUTES + " minutes.");
            notification.setType("INFO");
            notification.setActionUrl("http://localhost:5173/payment/" + payment.getId());
            notificationClientService.sendNotification(notification);

            log.info("Payment created successfully for order: {}", payment.getOrderId());

            return PaymentResponseDTO.builder()
                    .paymentId(payment.getId())
                    .clientSecret(paymentIntent.getClientSecret())
                    .paymentStatus(PaymentStatus.PENDING)
                    .expiresAt(payment.getExpiresAt())
                    .message("Payment created successfully")
                    .build();


        } catch (StripeException e) {
            log.error("Stripe payment processing failed for order: {}", paymentRequest.getOrderId(), e);
            releaseProductsForOrder(paymentRequest.getOrderId());
            return PaymentResponseDTO.builder()
                    .paymentStatus(PaymentStatus.FAILED)
                    .message(e.getMessage())
                    .build();
        }
    }

    @Override
    @Transactional
    public PaymentResponseDTO confirmPayment(String paymentIntentId) {
        try {
            log.info("Confirming payment for paymentIntentId: {}", paymentIntentId);


            Payment payment = paymentRepository.findByStripPaymentIntendId(paymentIntentId);
            if (payment == null) {
                throw new PaymentNotFoundException("Payment not found for intent: " + paymentIntentId);
            }

            // Update payment status
            payment.setPaymentStatus(PaymentStatus.COMPLETED);
            payment.setUpdatedAt(LocalDateTime.now());
            payment = paymentRepository.save(payment);

            // Update order status to confirmed
            orderClientService.updateOrderStatus(payment.getOrderId(), OrderStatus.CONFIRMED);

            // Send success notification
            NotificationSendDTO notification = new NotificationSendDTO();
            notification.setUserId(payment.getUserId());
            notification.setType("SUCCESS");
            notification.setMessage("Payment successful for order #" + payment.getOrderId());
            notification.setActionUrl("http://localhost:5173/orders/" + payment.getOrderId());
            notificationClientService.sendNotification(notification);

            return PaymentResponseDTO.builder()
                    .paymentId(payment.getId())
                    .paymentStatus(PaymentStatus.COMPLETED)
                    .message("Payment confirmed successfully")
                    .build();

        } catch (Exception e) {
            log.error("Payment confirmation failed for paymentIntentId: {}", paymentIntentId, e);

            Payment payment = paymentRepository.findByStripPaymentIntendId(paymentIntentId);
            if (payment != null) {
                releaseProductsForOrder(payment.getOrderId());

                // Send failure notification
                NotificationSendDTO notification = new NotificationSendDTO();
                notification.setUserId(payment.getUserId());
                notification.setType("ERROR");
                notification.setMessage("Payment failed for order #" + payment.getOrderId());
                notification.setActionUrl("http://localhost:5173/payment/retry/" + payment.getId());
                notificationClientService.sendNotification(notification);
            }

            return PaymentResponseDTO.builder()
                    .paymentStatus(PaymentStatus.FAILED)
                    .message("Payment confirmation failed: " + e.getMessage())
                    .build();
        }
    }

    @Override
    @Transactional
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

            // Update payment status
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            payment.setUpdatedAt(LocalDateTime.now());
            payment = paymentRepository.save(payment);

            // Release products back to inventory
            releaseProductsForOrder(payment.getOrderId());

            // Send refund notification
            NotificationSendDTO notification = new NotificationSendDTO();
            notification.setUserId(payment.getUserId());
            notification.setType("INFO");
            notification.setMessage("Refund processed for order #" + payment.getOrderId());
            notification.setActionUrl("http://localhost:5173/orders/" + payment.getOrderId());
            notificationClientService.sendNotification(notification);

            return PaymentResponseDTO.builder()
                    .paymentId(payment.getId())
                    .paymentStatus(PaymentStatus.REFUNDED)
                    .message("Payment refunded successfully")
                    .build();

        } catch (StripeException e) {
            log.error("Refund failed for payment: {}", paymentId, e);
            return PaymentResponseDTO.builder()
                    .paymentStatus(PaymentStatus.FAILED)
                    .message("Refund processing failed: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public PaymentResponseDTO getPaymentStatus(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));

        return PaymentResponseDTO.builder()
                .paymentId(payment.getId())
                .paymentStatus(payment.getPaymentStatus())
                .expiresAt(payment.getExpiresAt())
                .message("Payment status: " + payment.getPaymentStatus())
                .build();
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void handleExpiredPayments() {
        LocalDateTime now = LocalDateTime.now();
        List<Payment> expiredPayments = paymentRepository.findByPaymentStatusAndExpiresAtBefore(
                PaymentStatus.PENDING,
                now
        );

        for (Payment payment : expiredPayments) {
            try {
                // Release products back to inventory
                releaseProductsForOrder(payment.getOrderId());

                // Update payment status
                payment.setPaymentStatus(PaymentStatus.EXPIRED);
                payment.setUpdatedAt(now);
                paymentRepository.save(payment);

                // Update order status
                orderClientService.updateOrderStatus(payment.getOrderId(), OrderStatus.EXPIRED);

                // Send expiration notification
                NotificationSendDTO notification = new NotificationSendDTO();
                notification.setUserId(payment.getUserId());
                notification.setType("WARNING");
                notification.setMessage("Payment expired for order #" + payment.getOrderId());
                notification.setActionUrl("http://localhost:5173/payment/retry/" + payment.getId());
                notificationClientService.sendNotification(notification);

            } catch (Exception e) {
                log.error("Error handling expired payment: {}", payment.getId(), e);
            }
        }
    }


    public List<UserPaymentResponseDTO> getCompletedPaymentsForArtisan(String artisanId) {
        // Get orders for the artisan
        List<OrderResponseDTO> artisanOrders = orderClientService.getArtisansOrders(artisanId);

        // Filter and collect completed payments
        return artisanOrders.stream()
                .map(order -> {
                    try {
                        // Find payment by order ID and check if it's completed
                        Payment payment = paymentRepository.findByOrderId(order.getId());
                        if (payment != null && payment.getPaymentStatus() == PaymentStatus.COMPLETED) {
                            return UserPaymentResponseDTO.builder()
                                    .id(payment.getId())
                                    .paymentStatus(payment.getPaymentStatus())
                                    .amount(payment.getAmount())
                                    .stripPaymentIntendId(payment.getStripPaymentIntendId())
                                    .orderId(payment.getOrderId())
                                    .userId(payment.getUserId())
                                    .createdAt(LocalDateTime.now())
                                    .currency(payment.getCurrency())
                                    .expiresAt(payment.getExpiresAt())
                                    .build();
                        }
                        return null;
                    } catch (Exception e) {
                        log.error("Error finding payment for order: {}", order.getId(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<UserPaymentResponseDTO> findByUserId(String userId) {
        List<Payment> payments = paymentRepository.findByUserId(userId);
        return payments.stream()
                .map(payment -> UserPaymentResponseDTO.builder()
                        .id(payment.getId())
                        .paymentStatus(payment.getPaymentStatus())
                        .amount(payment.getAmount())
                        .stripPaymentIntendId(payment.getStripPaymentIntendId())
                        .orderId(payment.getOrderId())
                        .userId(payment.getUserId())
                        .createdAt(payment.getCreatedAt())
                        .currency(payment.getCurrency())
                        .expiresAt(payment.getExpiresAt())
                        .build())
                .collect(Collectors.toList());
    }

    private void releaseProductsForOrder(String orderId) {
        try {
            OrderResponseDTO order = orderClientService.getOrder(orderId);
            OrderItemResponseDTO item = order.getItem();
                try {
                    productClientService.releaseProduct(item.getProductId());
                } catch (Exception e) {
                    log.error("Failed to release product {} for order {}",
                            item.getProductId(), orderId, e);
                }

        } catch (Exception e) {
            log.error("Failed to release products for order {}", orderId, e);
        }
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