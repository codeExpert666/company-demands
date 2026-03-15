package com.example.betademands.dto;

import javax.validation.constraints.NotEmpty;

import java.util.List;

public record DeleteProblemTicketsRequest(
    @NotEmpty(message = "ids cannot be empty")
    List<Long> ids
) {
}
