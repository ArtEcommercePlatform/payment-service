package com.artztall.payment_service.service;

import com.artztall.payment_service.dto.OrderResponseDTO;
import com.artztall.payment_service.model.OrderStatus;
import com.artztall.payment_service.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderClientServiceTest {

    @Mock
    private WebClient orderServiceWebClient;

    @InjectMocks
    private OrderClientService orderClientService;

    private WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;
    private WebClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        responseSpec = mock(WebClient.ResponseSpec.class);

        //when(orderServiceWebClient.get()).thenReturn((WebClient.RequestHeadersUriSpec<?>) requestHeadersUriSpec);
       // when(orderServiceWebClient.put()).thenReturn((WebClient.RequestHeadersUriSpec<?>) requestHeadersUriSpec);
        //when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void testGetOrder_Success() {
        OrderResponseDTO mockResponse = createMockOrderResponse();

        when(responseSpec.bodyToMono(OrderResponseDTO.class)).thenReturn(Mono.just(mockResponse));

        OrderResponseDTO result = orderClientService.getOrder("order123");

        assertNotNull(result);
        assertEquals("order123", result.getId());
        assertEquals(BigDecimal.valueOf(100.00), result.getTotalAmount());
        verify(orderServiceWebClient).get();
        verify(requestHeadersUriSpec).uri("/api/orders/order123");
    }

    @Test
    void testGetOrder_NotFound() {
        when(responseSpec.bodyToMono(OrderResponseDTO.class)).thenThrow(WebClientResponseException.NotFound.class);

        assertThrows(WebClientResponseException.NotFound.class, () -> orderClientService.getOrder("invalidOrderId"));
        verify(orderServiceWebClient).get();
        verify(requestHeadersUriSpec).uri("/api/orders/invalidOrderId");
    }

    @Test
    void testUpdateOrderStatus_Success() {
        OrderResponseDTO updatedOrder = createMockOrderResponse();
        updatedOrder.setStatus(OrderStatus.SHIPPED);

        when(responseSpec.bodyToMono(OrderResponseDTO.class)).thenReturn(Mono.just(updatedOrder));

        orderClientService.updateOrderStatus("order123", OrderStatus.SHIPPED);

        verify(orderServiceWebClient).put();
        verify(requestHeadersUriSpec).uri(uriBuilder -> uriBuilder
                .path("/api/orders/{orderId}/status")
                .queryParam("status", OrderStatus.SHIPPED)
                .build("order123"));
    }

    @Test
    void testGetArtisansOrders_Success() {
        OrderResponseDTO mockOrder = createMockOrderResponse();

        // Use the correct ParameterizedTypeReference
        ParameterizedTypeReference<List<OrderResponseDTO>> typeReference =
                new ParameterizedTypeReference<List<OrderResponseDTO>>() {};

        when(responseSpec.bodyToMono(typeReference)).thenReturn(Mono.just(Collections.singletonList(mockOrder)));

        List<OrderResponseDTO> result = orderClientService.getArtisansOrders("artisan123");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("order123", result.get(0).getId());
        verify(orderServiceWebClient).get();
        verify(requestHeadersUriSpec).uri("/api/orders/artisan/artisan123");
    }

    @Test
    void testGetArtisansOrders_EmptyList() {
        // Use the correct ParameterizedTypeReference
        ParameterizedTypeReference<List<OrderResponseDTO>> typeReference =
                new ParameterizedTypeReference<List<OrderResponseDTO>>() {};

        when(responseSpec.bodyToMono(typeReference)).thenReturn(Mono.just(Collections.emptyList()));

        List<OrderResponseDTO> result = orderClientService.getArtisansOrders("artisan123");

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(orderServiceWebClient).get();
        verify(requestHeadersUriSpec).uri("/api/orders/artisan/artisan123");
    }

    // Helper method to create a mock OrderResponseDTO
    private OrderResponseDTO createMockOrderResponse() {
        OrderResponseDTO mockOrder = new OrderResponseDTO();
        mockOrder.setId("order123");
        mockOrder.setTotalAmount(BigDecimal.valueOf(100.00));
        mockOrder.setCreatedAt(LocalDateTime.now());
        mockOrder.setStatus(OrderStatus.PENDING);
        mockOrder.setPaymentStatus(PaymentStatus.PENDING);
        mockOrder.setUserId("user123");
        return mockOrder;
    }
}