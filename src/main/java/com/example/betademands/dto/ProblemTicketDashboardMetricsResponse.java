package com.example.betademands.dto;

import java.time.LocalDateTime;

public record ProblemTicketDashboardMetricsResponse(
    MetricCardResponse analysis,
    MetricCardResponse modify,
    MetricCardResponse push,
    MetricCardResponse closedLoop,
    LocalDateTime accurateFrom
) {
}
