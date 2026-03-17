package com.example.betademands.entity.enums;

public enum IssueStatus {
    ANALYZING("分析中"),
    FIXING("修改中"),
    PUSHING("版本推送中"),
    CLOSED("问题关闭"),
    SUSPENDED("挂起");

    private final String label;

    IssueStatus(String label) {
        this.label = label;
    }

    public static IssueStatus from(String value) {
        for (IssueStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown issue status: " + value);
    }

    public boolean isTerminal() {
        return this == CLOSED;
    }

    public String getLabel() {
        return label;
    }
}
