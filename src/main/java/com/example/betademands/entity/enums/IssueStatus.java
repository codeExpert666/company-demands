package com.example.betademands.entity.enums;

public enum IssueStatus {
    ANALYZING,
    FIXING,
    PUSHING,
    CLOSED,
    SUSPENDED;

    public static IssueStatus from(String value) {
        for (IssueStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown issue status: " + value);
    }

    public boolean isTerminal() {
        return this == CLOSED || this == SUSPENDED;
    }
}
