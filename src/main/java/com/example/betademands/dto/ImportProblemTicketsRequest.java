package com.example.betademands.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import java.util.List;

public record ImportProblemTicketsRequest(
    @NotEmpty(message = "items cannot be empty")
    List<@Valid CreateProblemTicketRequest> items
) {
}
