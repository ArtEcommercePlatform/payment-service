package com.artztall.payment_service.service;


import com.artztall.payment_service.dto.NotificationSendDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class NotificationClientService {
   private final WebClient notificationServiceWebClient;

   public void sendNotification(NotificationSendDTO notificationSendDTO) {
       notificationServiceWebClient.post()
               .uri("http://13.200.53.211:33001/api/notifications/send")
               .bodyValue(notificationSendDTO)
               .retrieve()
               .bodyToMono(NotificationSendDTO.class)
               .block();
   }

}
