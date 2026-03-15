package com.example.betademands.entity;

import com.example.betademands.entity.enums.IssueStatus;

import java.time.LocalDateTime;

public class ProblemTicketMetric {

    private Long ticketId;
    private LocalDateTime submitTime;
    private IssueStatus currentStatus;
    private LocalDateTime currentStageEnteredAt;
    private long analysisDurationSec;
    private boolean analysisCompleted;
    private long modifyDurationSec;
    private boolean modifyEligible;
    private long pushDurationSec;
    private boolean pushEligible;
    private Long closedLoopDurationSec;
    private boolean closedLoopCompleted;
    private boolean traceComplete;
    private LocalDateTime updatedAt;

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public LocalDateTime getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(LocalDateTime submitTime) {
        this.submitTime = submitTime;
    }

    public IssueStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(IssueStatus currentStatus) {
        this.currentStatus = currentStatus;
    }

    public LocalDateTime getCurrentStageEnteredAt() {
        return currentStageEnteredAt;
    }

    public void setCurrentStageEnteredAt(LocalDateTime currentStageEnteredAt) {
        this.currentStageEnteredAt = currentStageEnteredAt;
    }

    public long getAnalysisDurationSec() {
        return analysisDurationSec;
    }

    public void setAnalysisDurationSec(long analysisDurationSec) {
        this.analysisDurationSec = analysisDurationSec;
    }

    public boolean isAnalysisCompleted() {
        return analysisCompleted;
    }

    public void setAnalysisCompleted(boolean analysisCompleted) {
        this.analysisCompleted = analysisCompleted;
    }

    public long getModifyDurationSec() {
        return modifyDurationSec;
    }

    public void setModifyDurationSec(long modifyDurationSec) {
        this.modifyDurationSec = modifyDurationSec;
    }

    public boolean isModifyEligible() {
        return modifyEligible;
    }

    public void setModifyEligible(boolean modifyEligible) {
        this.modifyEligible = modifyEligible;
    }

    public long getPushDurationSec() {
        return pushDurationSec;
    }

    public void setPushDurationSec(long pushDurationSec) {
        this.pushDurationSec = pushDurationSec;
    }

    public boolean isPushEligible() {
        return pushEligible;
    }

    public void setPushEligible(boolean pushEligible) {
        this.pushEligible = pushEligible;
    }

    public Long getClosedLoopDurationSec() {
        return closedLoopDurationSec;
    }

    public void setClosedLoopDurationSec(Long closedLoopDurationSec) {
        this.closedLoopDurationSec = closedLoopDurationSec;
    }

    public boolean isClosedLoopCompleted() {
        return closedLoopCompleted;
    }

    public void setClosedLoopCompleted(boolean closedLoopCompleted) {
        this.closedLoopCompleted = closedLoopCompleted;
    }

    public boolean isTraceComplete() {
        return traceComplete;
    }

    public void setTraceComplete(boolean traceComplete) {
        this.traceComplete = traceComplete;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
