package com.example.betademands.entity;

public class DashboardMetricsAggregate {

    private long analysisAvgDurationSec;
    private long analysisSampleCount;
    private long modifyAvgDurationSec;
    private long modifySampleCount;
    private long pushAvgDurationSec;
    private long pushSampleCount;
    private long closedLoopAvgDurationSec;
    private long closedLoopSampleCount;

    public long getAnalysisAvgDurationSec() {
        return analysisAvgDurationSec;
    }

    public void setAnalysisAvgDurationSec(long analysisAvgDurationSec) {
        this.analysisAvgDurationSec = analysisAvgDurationSec;
    }

    public long getAnalysisSampleCount() {
        return analysisSampleCount;
    }

    public void setAnalysisSampleCount(long analysisSampleCount) {
        this.analysisSampleCount = analysisSampleCount;
    }

    public long getModifyAvgDurationSec() {
        return modifyAvgDurationSec;
    }

    public void setModifyAvgDurationSec(long modifyAvgDurationSec) {
        this.modifyAvgDurationSec = modifyAvgDurationSec;
    }

    public long getModifySampleCount() {
        return modifySampleCount;
    }

    public void setModifySampleCount(long modifySampleCount) {
        this.modifySampleCount = modifySampleCount;
    }

    public long getPushAvgDurationSec() {
        return pushAvgDurationSec;
    }

    public void setPushAvgDurationSec(long pushAvgDurationSec) {
        this.pushAvgDurationSec = pushAvgDurationSec;
    }

    public long getPushSampleCount() {
        return pushSampleCount;
    }

    public void setPushSampleCount(long pushSampleCount) {
        this.pushSampleCount = pushSampleCount;
    }

    public long getClosedLoopAvgDurationSec() {
        return closedLoopAvgDurationSec;
    }

    public void setClosedLoopAvgDurationSec(long closedLoopAvgDurationSec) {
        this.closedLoopAvgDurationSec = closedLoopAvgDurationSec;
    }

    public long getClosedLoopSampleCount() {
        return closedLoopSampleCount;
    }

    public void setClosedLoopSampleCount(long closedLoopSampleCount) {
        this.closedLoopSampleCount = closedLoopSampleCount;
    }
}
