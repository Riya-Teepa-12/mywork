package com.app.authservice.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscriptionPlansResponse {

    private boolean billingEnabled;
    private Integer freeAuthorPostLimit;
    private List<SubscriptionPlanInfoResponse> plans;
}

