package com.example.betademands.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDateTime;

@ConfigurationProperties(prefix = "problem-ticket.metrics")
public class ProblemTicketMetricsProperties {

    private LocalDateTime accurateFrom = LocalDateTime.of(2026, 3, 15, 0, 0);

    public LocalDateTime getAccurateFrom() {
        return accurateFrom;
    }

    public void setAccurateFrom(LocalDateTime accurateFrom) {
        this.accurateFrom = accurateFrom;
    }
}
