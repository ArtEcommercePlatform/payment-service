package com.artztall.payment_service.controller;

import com.artztall.payment_service.dto.PaymentRequestDTO;
import com.artztall.payment_service.dto.PaymentResponseDTO;
import com.artztall.payment_service.dto.UserPaymentResponseDTO;
import com.artztall.payment_service.model.PaymentStatus;
import com.artztall.payment_service.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class PaymentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    public void testCreatePayment() throws Exception {
        PaymentRequestDTO paymentRequestDTO = PaymentRequestDTO.builder()
                .orderId("order123")
                .userId("user123")
                .currency("USD")
                .paymentMethodId("pm123")
                .build();

        PaymentResponseDTO paymentResponseDTO = PaymentResponseDTO.builder()
                .paymentId("payment123")
                .orderId("order123")
                .clientSecret("secret123")
                .paymentStatus(PaymentStatus.PENDING)
                .amount(100.0)
                .message("Payment created successfully")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        when(paymentService.createPayment(paymentRequestDTO)).thenReturn(paymentResponseDTO);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value("payment123"))
                .andExpect(jsonPath("$.orderId").value("order123"))
                .andExpect(jsonPath("$.paymentStatus").value(PaymentStatus.PENDING.toString()));
    }

    @Test
    public void testConfirmPayment() throws Exception {
        String paymentIntentId = "payment123";
        PaymentResponseDTO paymentResponseDTO = PaymentResponseDTO.builder()
                .paymentId(paymentIntentId)
                .orderId("order123")
                .clientSecret("secret123")
                .paymentStatus(PaymentStatus.COMPLETED)
                .amount(100.0)
                .message("Payment confirmed successfully")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        when(paymentService.confirmPayment(paymentIntentId)).thenReturn(paymentResponseDTO);

        mockMvc.perform(post("/api/payments/confirm/{paymentIntentId}", paymentIntentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentIntentId))
                .andExpect(jsonPath("$.paymentStatus").value(PaymentStatus.COMPLETED.toString()));
    }

    @Test
    public void testRefundPayment() throws Exception {
        String paymentIntentId = "payment123";
        PaymentResponseDTO paymentResponseDTO = PaymentResponseDTO.builder()
                .paymentId(paymentIntentId)
                .orderId("order123")
                .clientSecret("secret123")
                .paymentStatus(PaymentStatus.REFUNDED)
                .amount(100.0)
                .message("Refund processed successfully")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        when(paymentService.refundPayment(paymentIntentId)).thenReturn(paymentResponseDTO);

        mockMvc.perform(post("/api/payments/payment/{paymentIntentId}", paymentIntentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentIntentId))
                .andExpect(jsonPath("$.paymentStatus").value("REFUNDED"));
    }

    @Test
    public void testGetPaymentStatus() throws Exception {
        String paymentIntentId = "payment123";
        PaymentResponseDTO paymentResponseDTO = PaymentResponseDTO.builder()
                .paymentId(paymentIntentId)
                .orderId("order123")
                .clientSecret("secret123")
                .paymentStatus(PaymentStatus.PENDING)
                .amount(100.0)
                .message("Payment status retrieved successfully")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        when(paymentService.getPaymentStatus(paymentIntentId)).thenReturn(paymentResponseDTO);

        mockMvc.perform(post("/api/payments/status/{paymentIntentId}", paymentIntentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentIntentId))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"));
    }

    @Test
    public void testGetCompletedPaymentsForArtisan() throws Exception {
        String artisanId = "artisan123";
        UserPaymentResponseDTO userPaymentResponseDTO = UserPaymentResponseDTO.builder()
                .id("payment123")
                .orderId("order123")
                .userId("user123")
                .amount(100L)
                .currency("USD")
                .paymentStatus(PaymentStatus.COMPLETED)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentService.getCompletedPaymentsForArtisan(artisanId))
                .thenReturn(Collections.singletonList(userPaymentResponseDTO));

        mockMvc.perform(get("/api/payments/artisan/{artisanId}", artisanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("payment123"))
                .andExpect(jsonPath("$[0].paymentStatus").value("COMPLETED"));
    }

    @Test
    public void testGetPaymentsByUser() throws Exception {
        String userId = "user123";
        UserPaymentResponseDTO userPaymentResponseDTO = UserPaymentResponseDTO.builder()
                .id("payment123")
                .orderId("order123")
                .userId(userId)
                .amount(100L)
                .currency("USD")
                .paymentStatus(PaymentStatus.COMPLETED)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentService.findByUserId(userId))
                .thenReturn(Collections.singletonList(userPaymentResponseDTO));

        mockMvc.perform(get("/api/payments/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId))
                .andExpect(jsonPath("$[0].paymentStatus").value("COMPLETED"));
    }
}
