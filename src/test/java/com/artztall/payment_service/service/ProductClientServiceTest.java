package com.artztall.payment_service.service;

import com.artztall.payment_service.dto.ProductAvailabilityRequest;
import com.artztall.payment_service.dto.ProductResponseDTO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ProductClientServiceTest {

    @Test
    void testReleaseProduct_SuccessfulResponse() {
        // Arrange
        WebClient mockWebClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        ProductClientService productClientService = new ProductClientService(mockWebClient);

        String productId = "12345";
        ProductResponseDTO responseDTO = new ProductResponseDTO(); // Mock or create a suitable response object

        // Mocking the WebClient behavior
        when(mockWebClient.put()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("/api/products/" + productId + "/release"))).thenReturn(requestBodySpec);
        //when(requestBodySpec.bodyValue(any(ProductAvailabilityRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ProductResponseDTO.class)).thenReturn(Mono.just(responseDTO));

        // Act
        productClientService.releaseProduct(productId);

        // Assert
        verify(mockWebClient.put(), times(1));
        verify(requestBodyUriSpec, times(1)).uri("/api/products/" + productId + "/release");
        verify(requestBodySpec, times(1)).bodyValue(any(ProductAvailabilityRequest.class));
        verify(responseSpec, times(1)).bodyToMono(ProductResponseDTO.class);
    }

    @Test
    void testReleaseProduct_UnsuccessfulResponse() {
        // Arrange
        WebClient mockWebClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        ProductClientService productClientService = new ProductClientService(mockWebClient);

        String productId = "12345";

        // Mocking the WebClient behavior
        when(mockWebClient.put()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("/api/products/" + productId + "/release"))).thenReturn(requestBodySpec);
        //when(requestBodySpec.bodyValue(any(ProductAvailabilityRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ProductResponseDTO.class)).thenReturn(Mono.error(
                WebClientResponseException.create(
                        400, "Bad Request", null, null, null
                )
        ));

        // Act & Assert
        WebClientResponseException exception = assertThrows(WebClientResponseException.class, () -> {
            productClientService.releaseProduct(productId);
        });

        assertEquals(400, exception.getRawStatusCode());
    }
}
