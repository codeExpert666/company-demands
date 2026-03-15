package com.example.betademands.entity.enums;

import java.util.Arrays;

public enum IssueStatus {
    ANALYZING,
    FIXING,
    PUSHING,
    CLOSED,
    SUSPENDED;

    public static IssueStatus from(String value) {
        return Arrays.stream(values())
            .filter(status -> status.name().equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown issue status: " + value));
    }

    public boolean isTerminal() {
        return this == CLOSED || this == SUSPENDED;
    }
}
