package com.artztall.payment_service.service;

import com.artztall.payment_service.dto.OrderResponseDTO;
import com.artztall.payment_service.dto.PaymentRequestDTO;
import com.artztall.payment_service.dto.PaymentResponseDTO;
import com.artztall.payment_service.exception.PaymentNotFoundException;
import com.artztall.payment_service.model.OrderStatus;
import com.artztall.payment_service.model.Payment;
import com.artztall.payment_service.model.PaymentStatus;
import com.artztall.payment_service.repository.PaymentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PaymentServiceImplTest {

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderClientService orderClientService;

    @Mock
    private ProductClientService productClientService;

    @Mock
    private NotificationClientService notificationClientService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreatePayment_Success() throws StripeException {
        // Mock data
        PaymentRequestDTO paymentRequestDTO = new PaymentRequestDTO();
        paymentRequestDTO.setOrderId("order_123");
        paymentRequestDTO.setUserId("user_456");
        paymentRequestDTO.setCurrency("USD");
        paymentRequestDTO.setPaymentMethodId("pm_card_visa");

        OrderResponseDTO orderResponseDTO = new OrderResponseDTO();
        orderResponseDTO.setTotalAmount(BigDecimal.valueOf(100.00));

        PaymentIntent paymentIntent = mock(PaymentIntent.class);
        when(paymentIntent.getId()).thenReturn("pi_test");
        when(paymentIntent.getClientSecret()).thenReturn("secret_test");

        when(orderClientService.getOrder(eq("order_123"))).thenReturn(orderResponseDTO);
        when(PaymentIntent.create((Map<String, Object>) any(), any(RequestOptions.class))).thenReturn(paymentIntent);

        Payment payment = new Payment();
        payment.setId("payment_123");
        payment.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        payment.setPaymentStatus(PaymentStatus.PENDING);

        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // Act
        PaymentResponseDTO response = paymentService.createPayment(paymentRequestDTO);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.PENDING, response.getPaymentStatus());
        assertEquals("secret_test", response.getClientSecret());

        verify(notificationClientService, times(1)).sendNotification(any());
    }

    @Test
    void testCreatePayment_StripeException() throws StripeException {
        // Mock data
        PaymentRequestDTO paymentRequestDTO = new PaymentRequestDTO();
        paymentRequestDTO.setOrderId("order_123");
        paymentRequestDTO.setUserId("user_456");
        paymentRequestDTO.setCurrency("USD");
        paymentRequestDTO.setPaymentMethodId("pm_card_visa");

        OrderResponseDTO orderResponseDTO = new OrderResponseDTO();
        orderResponseDTO.setTotalAmount(BigDecimal.valueOf(100.00));

        when(orderClientService.getOrder(eq("order_123"))).thenReturn(orderResponseDTO);
        when(PaymentIntent.create((Map<String, Object>) any(), any(RequestOptions.class))).thenThrow(StripeException.class);

        // Act
        PaymentResponseDTO response = paymentService.createPayment(paymentRequestDTO);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getPaymentStatus());

        verify(productClientService, times(1)).releaseProduct(any());
    }

    @Test
    void testConfirmPayment_Success() {
        // Mock data
        String paymentIntentId = "pi_test";

        Payment payment = new Payment();
        payment.setId("payment_123");
        payment.setOrderId("order_123");
        payment.setUserId("user_456");
        payment.setPaymentStatus(PaymentStatus.PENDING);

        when(paymentRepository.findByStripPaymentIntendId(eq(paymentIntentId))).thenReturn(payment);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // Act
        PaymentResponseDTO response = paymentService.confirmPayment(paymentIntentId);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.COMPLETED, response.getPaymentStatus());

        verify(orderClientService, times(1)).updateOrderStatus(eq("order_123"), eq(OrderStatus.CONFIRMED));
        verify(notificationClientService, times(1)).sendNotification(any());
    }

    @Test
    void testConfirmPayment_NotFound() {
        // Mock data
        String paymentIntentId = "pi_invalid";
        when(paymentRepository.findByStripPaymentIntendId(eq(paymentIntentId))).thenReturn(null);

        // Act & Assert
        assertThrows(PaymentNotFoundException.class, () -> paymentService.confirmPayment(paymentIntentId));
    }

    @Test
    void testRefundPayment_Success() throws StripeException {
        // Mock data
        String paymentId = "payment_123";

        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setOrderId("order_123");
        payment.setPaymentStatus(PaymentStatus.COMPLETED);
        payment.setStripPaymentIntendId("pi_test");

        when(paymentRepository.findById(eq(paymentId))).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // Act
        PaymentResponseDTO response = paymentService.refundPayment(paymentId);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.REFUNDED, response.getPaymentStatus());

        verify(productClientService, times(1)).releaseProduct(any());
        verify(notificationClientService, times(1)).sendNotification(any());
    }

    @Test
    void testRefundPayment_NotFound() {
        // Mock data
        String paymentId = "invalid_payment";
        when(paymentRepository.findById(eq(paymentId))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(PaymentNotFoundException.class, () -> paymentService.refundPayment(paymentId));
    }
}
