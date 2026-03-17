package com.example.betademands.dto;

import com.example.betademands.entity.enums.IssueStatus;
import javax.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record CreateProblemTicketRequest(
    @NotBlank(message = "ticketNo cannot be blank")
    String ticketNo,
    @NotBlank(message = "description cannot be blank")
    String description,
    LocalDateTime submitTime,
    IssueStatus status
) {
}
