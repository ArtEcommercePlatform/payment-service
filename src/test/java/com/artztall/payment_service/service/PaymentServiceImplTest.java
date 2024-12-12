package com.artztall.payment_service.service;

import com.artztall.payment_service.dto.*;
import com.artztall.payment_service.exception.PaymentProcessingException;
import com.artztall.payment_service.model.Payment;
import com.artztall.payment_service.model.PaymentStatus;
import com.artztall.payment_service.model.OrderStatus;
import com.artztall.payment_service.repository.PaymentRepository;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.net.RequestOptions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderClientService orderClientService;

    @Mock
    private ProductClientService productClientService;

    @Mock
    private NotificationClientService notificationClientService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private PaymentRequestDTO validPaymentRequest;
    private OrderResponseDTO validOrderResponse;

    private MockedStatic<PaymentIntent> paymentIntentMock;
    private MockedStatic<Refund> refundMock;

    @BeforeEach
    void setUp() {
        validPaymentRequest = PaymentRequestDTO.builder()
                .orderId("order-123")
                .userId("user-456")
                .currency("USD")
                .paymentMethodId("pm_card_visa")
                .build();

        validOrderResponse = OrderResponseDTO.builder()
                .id("order-123")
                .totalAmount(BigDecimal.valueOf(100.00))
                .item(OrderItemResponseDTO.builder()
                        .productId("product-789")
                        .build())
                .build();

        paymentIntentMock = mockStatic(PaymentIntent.class);
        refundMock = mockStatic(Refund.class);
    }

    @AfterEach
    void tearDown() {
        // Close the static mocks after each test to avoid conflicts
        paymentIntentMock.close();
        refundMock.close();
    }

    @Test
    void createPayment_Success() throws Exception {
        // Mocking order client and Stripe interactions
        when(orderClientService.getOrder(anyString())).thenReturn(validOrderResponse);

        PaymentIntent mockPaymentIntent = mock(PaymentIntent.class);
        when(mockPaymentIntent.getId()).thenReturn("pi_123456");
        when(mockPaymentIntent.getClientSecret()).thenReturn("client_secret_123");

        // Mock PaymentIntent.create to return the mock payment intent
        paymentIntentMock.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
                .thenReturn(mockPaymentIntent);

        // Mock the saving of the Payment object
        Payment mockPayment = Payment.builder()
                .id("payment-123")
                .orderId(validPaymentRequest.getOrderId())
                .userId(validPaymentRequest.getUserId())
                .paymentStatus(PaymentStatus.PENDING)
                .stripPaymentIntendId(mockPaymentIntent.getId())
                .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment); // Mocking the repository save

        // Call the method to test
        PaymentResponseDTO response = paymentService.createPayment(validPaymentRequest);

        // Verify interactions
        verify(orderClientService).getOrder(validPaymentRequest.getOrderId());
        verify(paymentRepository).save(any(Payment.class));
        verify(notificationClientService).sendNotification(any(NotificationSendDTO.class));

        // Assertions
        assertNotNull(response);
        assertEquals(PaymentStatus.PENDING, response.getPaymentStatus());
        assertNotNull(response.getClientSecret());
    }


    @Test
    void createPayment_StripeException() throws Exception {
        // Mocking order client to throw Stripe exception
        when(orderClientService.getOrder(anyString())).thenReturn(validOrderResponse);

        // Mock PaymentIntent.create to throw a StripeException
//        mockStatic(PaymentIntent.class);
        when(PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
                .thenThrow(new com.stripe.exception.CardException("Stripe Error", null, null, null, null, null, null,null));

        PaymentResponseDTO response = paymentService.createPayment(validPaymentRequest);

        // Verify interactions
        verify(productClientService).releaseProduct(validOrderResponse.getItem().getProductId());

        // Assertions
        assertEquals(PaymentStatus.FAILED, response.getPaymentStatus());
        assertNotNull(response.getMessage());
    }

    @Test
    void refundPayment_Success() throws Exception {
        // Ensure mock payment setup
        Payment mockPayment = Payment.builder()
                .id("payment-123")
                .orderId("order-123")
                .userId("user-456")
                .stripPaymentIntendId("pi_123456") // Stripe payment intent ID
                .paymentStatus(PaymentStatus.COMPLETED)
                .build();

        // Mock the payment repository to return the mock payment
        when(paymentRepository.findById(anyString())).thenReturn(Optional.of(mockPayment));

        // Mock the Refund.create to return a mock Refund
        Refund mockRefund = mock(Refund.class);
        refundMock.when(() -> Refund.create(any(RefundCreateParams.class), any(RequestOptions.class)))
                .thenReturn(mockRefund);

        // Mock the save method to return the updated payment
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        // Mock the release products method
        doNothing().when(productClientService).releaseProduct(anyString());

        // Ensure that the mock order response is returned
        when(orderClientService.getOrder("order-123")).thenReturn(validOrderResponse);

        // Mock the notification service
        doNothing().when(notificationClientService).sendNotification(any(NotificationSendDTO.class));

        // Call the refundPayment method
        PaymentResponseDTO response = paymentService.refundPayment("payment-123");

        // Verify interactions
        verify(paymentRepository).findById("payment-123");
        verify(productClientService).releaseProduct(validOrderResponse.getItem().getProductId()); // Correct verification
        verify(paymentRepository).save(mockPayment);
        verify(notificationClientService).sendNotification(any(NotificationSendDTO.class));

        // Assertions
        assertEquals(PaymentStatus.REFUNDED, response.getPaymentStatus());
        assertEquals("Payment refunded successfully", response.getMessage());
    }





    @Test
    void refundPayment_NotCompletedPayment() {
        // Prepare mock payment that is not completed
        Payment mockPayment = Payment.builder()
                .id("payment-123")
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findById(anyString())).thenReturn(Optional.of(mockPayment));

        // Assert that an exception is thrown
        assertThrows(PaymentProcessingException.class,
                () -> paymentService.refundPayment("payment-123"));
    }

    @Test
    void getPaymentStatus_Success() {
        // Prepare mock payment
        Payment mockPayment = Payment.builder()
                .id("payment-123")
                .paymentStatus(PaymentStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        when(paymentRepository.findById(anyString())).thenReturn(Optional.of(mockPayment));

        PaymentResponseDTO response = paymentService.getPaymentStatus("payment-123");

        // Assertions
        assertEquals("payment-123", response.getPaymentId());
        assertEquals(PaymentStatus.PENDING, response.getPaymentStatus());
        assertNotNull(response.getExpiresAt());
    }

    @Test
    void handleExpiredPayments_Success() {
        // Prepare expired payments
        Payment expiredPayment = Payment.builder()
                .id("payment-123")
                .orderId("order-123")
                .userId("user-456")
                .paymentStatus(PaymentStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusMinutes(30))
                .build();

        when(paymentRepository.findByPaymentStatusAndExpiresAtBefore(
                eq(PaymentStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(expiredPayment));

        when(orderClientService.getOrder(anyString())).thenReturn(validOrderResponse);


        // Call the method
        paymentService.handleExpiredPayments();

        // Verify interactions
        verify(productClientService).releaseProduct(anyString());
        verify(paymentRepository).save(expiredPayment);
        verify(orderClientService).updateOrderStatus(expiredPayment.getOrderId(), OrderStatus.EXPIRED);
        verify(notificationClientService).sendNotification(any(NotificationSendDTO.class));
    }


    private PaymentRequestDTO createModifiedPaymentRequest(String fieldToRemove) {
        PaymentRequestDTO.PaymentRequestDTOBuilder builder = PaymentRequestDTO.builder()
                .orderId("order-123")
                .userId("user-456")
                .currency("USD")
                .paymentMethodId("pm_card_visa");

        switch(fieldToRemove) {
            case "currency":
                builder.currency(null);
                break;
            case "paymentMethodId":
                builder.paymentMethodId(null);
                break;
            case "orderId":
                builder.orderId(null);
                break;
            case "userId":
                builder.userId(null);
                break;
        }

        return builder.build();
    }

    @Test
    void validatePaymentRequest_MissingCurrency() {
        PaymentRequestDTO invalidRequest = createModifiedPaymentRequest("currency");

        assertThrows(IllegalArgumentException.class,
                () -> paymentService.createPayment(invalidRequest));
    }

    @Test
    void validatePaymentRequest_MissingPaymentMethodId() {
        PaymentRequestDTO invalidRequest = createModifiedPaymentRequest("paymentMethodId");

        assertThrows(IllegalArgumentException.class,
                () -> paymentService.createPayment(invalidRequest));
    }
}