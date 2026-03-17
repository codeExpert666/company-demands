package com.example.betademands.entity;

import com.example.betademands.entity.enums.IssueStatus;

import java.time.LocalDateTime;

public class ProblemTicketMetric {

    private Long ticketId;
    private LocalDateTime submitTime;
    private IssueStatus currentStatus;
    private LocalDateTime currentStageEnteredAt;
    private long analysisDurationSec;
    private boolean analysisTrackable;
    private long modifyDurationSec;
    private boolean modifyTrackable;
    private long pushDurationSec;
    private boolean pushTrackable;
    private Long closedLoopDurationSec;
    private boolean closedLoopCompleted;
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

    public boolean isAnalysisTrackable() {
        return analysisTrackable;
    }

    public void setAnalysisTrackable(boolean analysisTrackable) {
        this.analysisTrackable = analysisTrackable;
    }

    public long getModifyDurationSec() {
        return modifyDurationSec;
    }

    public void setModifyDurationSec(long modifyDurationSec) {
        this.modifyDurationSec = modifyDurationSec;
    }

    public boolean isModifyTrackable() {
        return modifyTrackable;
    }

    public void setModifyTrackable(boolean modifyTrackable) {
        this.modifyTrackable = modifyTrackable;
    }

    public long getPushDurationSec() {
        return pushDurationSec;
    }

    public void setPushDurationSec(long pushDurationSec) {
        this.pushDurationSec = pushDurationSec;
    }

    public boolean isPushTrackable() {
        return pushTrackable;
    }

    public void setPushTrackable(boolean pushTrackable) {
        this.pushTrackable = pushTrackable;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
