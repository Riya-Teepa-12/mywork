package com.app.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DispatchResponse {
    private String message;
    private long count;
}
