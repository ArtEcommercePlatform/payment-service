package com.artztall.payment_service.service;

import com.artztall.payment_service.dto.NotificationSendDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.reactivestreams.Publisher;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;

class NotificationClientServiceTest {

    @InjectMocks
    private NotificationClientService notificationClientService;

    @Mock
    private WebClient notificationServiceWebClient;

    @Mock
    private RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private RequestHeadersSpec requestHeadersSpec;

    @Mock
    private ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void sendNotification_shouldCallNotificationService() {
        // Arrange
        NotificationSendDTO notificationSendDTO = new NotificationSendDTO();
        notificationSendDTO.setUserId("123");
        notificationSendDTO.setMessage("Test Message");
        notificationSendDTO.setType("INFO");
        notificationSendDTO.setActionUrl("http://example.com");

        when(notificationServiceWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/notifications/send")).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(notificationSendDTO)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(NotificationSendDTO.class)).thenReturn((Mono<NotificationSendDTO>) mock(Publisher.class));

        // Act
        notificationClientService.sendNotification(notificationSendDTO);

        // Assert
        verify(notificationServiceWebClient).post();
        verify(requestBodyUriSpec).uri("/api/notifications/send");
        verify(requestBodySpec).bodyValue(notificationSendDTO);
        verify(requestHeadersSpec).retrieve();
        verify(responseSpec).bodyToMono(NotificationSendDTO.class);
    }
}
