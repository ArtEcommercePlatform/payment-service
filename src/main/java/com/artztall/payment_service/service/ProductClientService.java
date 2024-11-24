package com.artztall.payment_service.service;


import com.artztall.payment_service.dto.ProductAvailabilityRequest;
import com.artztall.payment_service.dto.ProductResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;


@Service
@RequiredArgsConstructor
public class ProductClientService {
    private final WebClient productServiceWebClient;

    public void releaseProduct(String productId) {
        productServiceWebClient.put()
                .uri("/api/products/" + productId + "/release")
                .bodyValue(new ProductAvailabilityRequest(true))
                .retrieve()
                .bodyToMono(ProductResponseDTO.class)
                .block();
    }
}