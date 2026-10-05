package com.app.newsletterservice.dto;

import java.util.List;

import com.app.newsletterservice.entity.SubscriberStatus;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendNewsletterRequest {
    @NotBlank
    private String subject;
    @NotBlank
    private String content;
    private SubscriberStatus statusFilter;
    private List<String> preferenceFilter;
}
