package com.app.authservice.dto;

import com.app.authservice.entity.Role;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscriptionEntitlementResponse {

    private Long userId;
    private Role role;
    private boolean admin;
    private boolean authorPostSubscriptionActive;
    private boolean newsletterSubscriptionActive;
    private boolean newsletterIncludedByAuthorPlan;
    private boolean newsletterEntitled;
    private boolean canCreateAuthorPosts;
    private Integer freeAuthorPostLimit;
    private Long freePostsUsed;
    private Long freePostsRemaining;
}

