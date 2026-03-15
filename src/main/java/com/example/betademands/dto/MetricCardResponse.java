package com.example.betademands.dto;

public record MetricCardResponse(
    long avgDurationSec,
    long sampleCount
) {
}
