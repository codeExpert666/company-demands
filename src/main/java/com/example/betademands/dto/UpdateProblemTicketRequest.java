package com.example.betademands.dto;

import com.example.betademands.entity.enums.IssueStatus;

import java.time.LocalDateTime;

public record UpdateProblemTicketRequest(
    String ticketNo,
    String description,
    LocalDateTime submitTime,
    IssueStatus status
) {
}
