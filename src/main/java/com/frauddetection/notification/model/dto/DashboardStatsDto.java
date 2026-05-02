package com.frauddetection.notification.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {

    private long totalProcessed;
    private long totalApproved;
    private long totalBlocked;
    private long totalReview;
    private long decidedByRuleEngine;
    private long decidedByAiAgent;
}