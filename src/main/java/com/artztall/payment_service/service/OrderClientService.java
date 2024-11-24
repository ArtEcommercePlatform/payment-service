package com.artztall.payment_service.service;


import com.artztall.payment_service.dto.OrderResponseDTO;
import com.artztall.payment_service.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class OrderClientService {
    private final WebClient orderServiceWebClient;
    public OrderResponseDTO getOrder(String orderId) {
        return orderServiceWebClient.get()
                .uri("/api/orders/" + orderId)
                .retrieve()
                .bodyToMono(OrderResponseDTO.class)
                .block();
    }

    public void updateOrderStatus(String orderId, OrderStatus orderStatus) {
        orderServiceWebClient.post()
                .uri("/api/orders/" + orderId)
                .bodyValue(orderStatus)
                .retrieve()
                .bodyToMono(OrderResponseDTO.class)
                .block();
    }
}
