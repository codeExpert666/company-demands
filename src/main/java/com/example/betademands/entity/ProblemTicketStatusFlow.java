package com.example.betademands.entity;

import com.example.betademands.entity.enums.FlowSource;
import com.example.betademands.entity.enums.IssueStatus;

import java.time.LocalDateTime;

public class ProblemTicketStatusFlow {

    private Long id;
    private Long ticketId;
    private IssueStatus fromStatus;
    private IssueStatus toStatus;
    private LocalDateTime operateTime;
    private Long operatorId;
    private FlowSource source;
    private String remark;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public IssueStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(IssueStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public IssueStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(IssueStatus toStatus) {
        this.toStatus = toStatus;
    }

    public LocalDateTime getOperateTime() {
        return operateTime;
    }

    public void setOperateTime(LocalDateTime operateTime) {
        this.operateTime = operateTime;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public FlowSource getSource() {
        return source;
    }

    public void setSource(FlowSource source) {
        this.source = source;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
