package com.example.betademands.dto;

import com.example.betademands.entity.enums.IssueStatus;
import javax.validation.constraints.NotNull;

public record ChangeProblemTicketStatusRequest(
    @NotNull(message = "status cannot be null")
    IssueStatus status,
    String remark
) {
}
