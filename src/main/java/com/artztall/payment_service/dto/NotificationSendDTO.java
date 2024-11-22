package com.artztall.payment_service.dto;


import lombok.Builder;
import lombok.Data;

@Data
public class NotificationSendDTO {

    private String userId;
    private String message;
    private String type;
    private String actionUrl;
}
