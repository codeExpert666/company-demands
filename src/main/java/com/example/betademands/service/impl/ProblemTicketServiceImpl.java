package com.example.betademands.service.impl;

import com.example.betademands.config.ProblemTicketMetricsProperties;
import com.example.betademands.dto.ChangeProblemTicketStatusRequest;
import com.example.betademands.dto.CreateProblemTicketRequest;
import com.example.betademands.dto.MetricCardResponse;
import com.example.betademands.dto.ProblemTicketDashboardMetricsResponse;
import com.example.betademands.dto.ProblemTicketResponse;
import com.example.betademands.dto.UpdateProblemTicketRequest;
import com.example.betademands.entity.DashboardMetricsAggregate;
import com.example.betademands.entity.ProblemTicket;
import com.example.betademands.entity.ProblemTicketMetric;
import com.example.betademands.entity.ProblemTicketStatusFlow;
import com.example.betademands.entity.enums.FlowSource;
import com.example.betademands.entity.enums.IssueStatus;
import com.example.betademands.exception.BusinessException;
import com.example.betademands.mapper.ProblemTicketMapper;
import com.example.betademands.mapper.ProblemTicketMetricMapper;
import com.example.betademands.mapper.ProblemTicketStatusFlowMapper;
import com.example.betademands.service.ProblemTicketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ProblemTicketServiceImpl implements ProblemTicketService {

    private static final Map<IssueStatus, Set<IssueStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(IssueStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(IssueStatus.ANALYZING, EnumSet.of(IssueStatus.FIXING, IssueStatus.CLOSED, IssueStatus.SUSPENDED));
        ALLOWED_TRANSITIONS.put(IssueStatus.FIXING, EnumSet.of(IssueStatus.PUSHING, IssueStatus.ANALYZING));
        ALLOWED_TRANSITIONS.put(IssueStatus.PUSHING, EnumSet.of(IssueStatus.CLOSED, IssueStatus.FIXING, IssueStatus.ANALYZING));
        ALLOWED_TRANSITIONS.put(IssueStatus.CLOSED, EnumSet.noneOf(IssueStatus.class));
        ALLOWED_TRANSITIONS.put(IssueStatus.SUSPENDED, EnumSet.noneOf(IssueStatus.class));
    }

    private final ProblemTicketMapper problemTicketMapper;
    private final ProblemTicketStatusFlowMapper flowMapper;
    private final ProblemTicketMetricMapper metricMapper;
    private final ProblemTicketMetricsProperties metricsProperties;
    private final Clock clock;

    public ProblemTicketServiceImpl(ProblemTicketMapper problemTicketMapper,
                                    ProblemTicketStatusFlowMapper flowMapper,
                                    ProblemTicketMetricMapper metricMapper,
                                    ProblemTicketMetricsProperties metricsProperties,
                                    Clock clock) {
        this.problemTicketMapper = problemTicketMapper;
        this.flowMapper = flowMapper;
        this.metricMapper = metricMapper;
        this.metricsProperties = metricsProperties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ProblemTicketResponse createTicket(CreateProblemTicketRequest request, Long operatorId, FlowSource source) {
        LocalDateTime submitTime = request.submitTime() == null ? now() : request.submitTime();
        ProblemTicket ticket = new ProblemTicket();
        ticket.setTicketNo(request.ticketNo());
        ticket.setDescription(request.description());
        ticket.setSubmitTime(submitTime);
        ticket.setStatus(IssueStatus.ANALYZING);
        ticket.setStatusChangedAt(submitTime);
        ticket.setDeleted(false);
        ticket.setCreatedAt(now());
        ticket.setUpdatedAt(now());
        problemTicketMapper.insert(ticket);

        insertFlow(ticket.getId(), null, IssueStatus.ANALYZING, submitTime, operatorId, source, "initial status");
        metricMapper.insert(initMetric(ticket.getId(), submitTime, IssueStatus.ANALYZING, true));

        return toResponse(problemTicketMapper.selectActiveById(ticket.getId()));
    }

    @Override
    @Transactional
    public List<ProblemTicketResponse> importTickets(List<CreateProblemTicketRequest> requests, Long operatorId) {
        if (CollectionUtils.isEmpty(requests)) {
            throw new BusinessException("import items cannot be empty");
        }
        return requests.stream()
            .map(request -> createTicket(request, operatorId, FlowSource.IMPORT))
            .toList();
    }

    @Override
    @Transactional
    public ProblemTicketResponse updateTicket(Long ticketId, UpdateProblemTicketRequest request, Long operatorId) {
        ProblemTicket ticket = requireActiveTicket(ticketId);
        if (request.submitTime() != null && !request.submitTime().equals(ticket.getSubmitTime())) {
            throw new BusinessException("submitTime cannot be changed after creation");
        }
        if (request.ticketNo() != null && request.ticketNo().isBlank()) {
            throw new BusinessException("ticketNo cannot be blank");
        }
        if (request.description() != null && request.description().isBlank()) {
            throw new BusinessException("description cannot be blank");
        }

        boolean changed = false;
        ProblemTicket update = new ProblemTicket();
        update.setId(ticketId);
        update.setUpdatedAt(now());

        if (request.ticketNo() != null && !request.ticketNo().equals(ticket.getTicketNo())) {
            update.setTicketNo(request.ticketNo());
            changed = true;
        }
        if (request.description() != null && !request.description().equals(ticket.getDescription())) {
            update.setDescription(request.description());
            changed = true;
        }
        if (changed) {
            problemTicketMapper.updateSelective(update);
        }

        if (request.status() != null && request.status() != ticket.getStatus()) {
            changeStatusInternal(ticketId, request.status(), null, operatorId);
        }

        return toResponse(problemTicketMapper.selectActiveById(ticketId));
    }

    @Override
    @Transactional
    public ProblemTicketResponse changeStatus(Long ticketId, ChangeProblemTicketStatusRequest request, Long operatorId) {
        changeStatusInternal(ticketId, request.status(), request.remark(), operatorId);
        return toResponse(problemTicketMapper.selectActiveById(ticketId));
    }

    @Override
    @Transactional
    public void deleteTickets(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new BusinessException("ids cannot be empty");
        }
        long activeCount = problemTicketMapper.countActiveByIds(ids);
        if (activeCount != ids.size()) {
            throw new BusinessException("some tickets do not exist");
        }
        flowMapper.deleteByTicketIds(ids);
        metricMapper.deleteByTicketIds(ids);
        problemTicketMapper.deleteByIds(ids);
    }

    @Override
    public ProblemTicketDashboardMetricsResponse getDashboardMetrics() {
        DashboardMetricsAggregate aggregate = metricMapper.aggregateDashboardMetrics();
        return new ProblemTicketDashboardMetricsResponse(
            new MetricCardResponse(aggregate.getAnalysisAvgDurationSec(), aggregate.getAnalysisSampleCount()),
            new MetricCardResponse(aggregate.getModifyAvgDurationSec(), aggregate.getModifySampleCount()),
            new MetricCardResponse(aggregate.getPushAvgDurationSec(), aggregate.getPushSampleCount()),
            new MetricCardResponse(aggregate.getClosedLoopAvgDurationSec(), aggregate.getClosedLoopSampleCount()),
            metricsProperties.getAccurateFrom()
        );
    }

    @Override
    @Transactional
    public int backfillMissingMetrics() {
        List<ProblemTicket> tickets = problemTicketMapper.selectWithoutMetrics();
        int count = 0;
        for (ProblemTicket ticket : tickets) {
            boolean traceComplete = ticket.getStatus() == IssueStatus.ANALYZING;
            ProblemTicketMetric metric = initMetric(
                ticket.getId(),
                ticket.getSubmitTime(),
                ticket.getStatus(),
                traceComplete
            );
            metric.setCurrentStageEnteredAt(traceComplete ? ticket.getSubmitTime() : ticket.getStatusChangedAt());
            metricMapper.insert(metric);
            if (traceComplete) {
                insertFlow(ticket.getId(), null, IssueStatus.ANALYZING, ticket.getSubmitTime(), 0L, FlowSource.BACKFILL, "backfill initial analyzing state");
            }
            count++;
        }
        return count;
    }

    private void changeStatusInternal(Long ticketId, IssueStatus targetStatus, String remark, Long operatorId) {
        ProblemTicket ticket = requireActiveTicketForUpdate(ticketId);
        if (ticket.getStatus() == targetStatus) {
            throw new BusinessException("duplicate status change is not allowed");
        }
        validateTransition(ticket.getStatus(), targetStatus);

        ProblemTicketMetric metric = metricMapper.selectByTicketIdForUpdate(ticketId);
        if (metric == null) {
            throw new BusinessException("metric record not found for ticket " + ticketId);
        }

        LocalDateTime operationTime = now();
        long segmentSec = ChronoUnit.SECONDS.between(metric.getCurrentStageEnteredAt(), operationTime);
        if (segmentSec < 0) {
            throw new BusinessException("status change time is earlier than current stage start time");
        }

        if (ticket.getStatus() == IssueStatus.ANALYZING) {
            metric.setAnalysisDurationSec(metric.getAnalysisDurationSec() + segmentSec);
            metric.setAnalysisCompleted(true);
        } else if (ticket.getStatus() == IssueStatus.FIXING) {
            metric.setModifyDurationSec(metric.getModifyDurationSec() + segmentSec);
            if (targetStatus == IssueStatus.PUSHING) {
                metric.setModifyEligible(true);
            }
        } else if (ticket.getStatus() == IssueStatus.PUSHING) {
            metric.setPushDurationSec(metric.getPushDurationSec() + segmentSec);
            if (targetStatus == IssueStatus.CLOSED) {
                metric.setPushEligible(true);
            }
        }

        if (targetStatus.isTerminal()) {
            metric.setClosedLoopDurationSec(ChronoUnit.SECONDS.between(metric.getSubmitTime(), operationTime));
            metric.setClosedLoopCompleted(true);
        } else {
            metric.setClosedLoopDurationSec(null);
            metric.setClosedLoopCompleted(false);
        }

        metric.setCurrentStatus(targetStatus);
        metric.setCurrentStageEnteredAt(operationTime);
        metric.setUpdatedAt(operationTime);
        metricMapper.update(metric);

        problemTicketMapper.updateStatus(ticketId, targetStatus.name(), operationTime, operationTime);
        insertFlow(ticketId, ticket.getStatus(), targetStatus, operationTime, operatorId, FlowSource.STATUS_UPDATE, remark);
    }

    private ProblemTicketMetric initMetric(Long ticketId, LocalDateTime submitTime, IssueStatus currentStatus, boolean traceComplete) {
        ProblemTicketMetric metric = new ProblemTicketMetric();
        metric.setTicketId(ticketId);
        metric.setSubmitTime(submitTime);
        metric.setCurrentStatus(currentStatus);
        metric.setCurrentStageEnteredAt(submitTime);
        metric.setAnalysisDurationSec(0);
        metric.setAnalysisCompleted(false);
        metric.setModifyDurationSec(0);
        metric.setModifyEligible(false);
        metric.setPushDurationSec(0);
        metric.setPushEligible(false);
        metric.setClosedLoopDurationSec(null);
        metric.setClosedLoopCompleted(false);
        metric.setTraceComplete(traceComplete);
        metric.setUpdatedAt(now());
        return metric;
    }

    private void insertFlow(Long ticketId,
                            IssueStatus fromStatus,
                            IssueStatus toStatus,
                            LocalDateTime operateTime,
                            Long operatorId,
                            FlowSource source,
                            String remark) {
        ProblemTicketStatusFlow flow = new ProblemTicketStatusFlow();
        flow.setTicketId(ticketId);
        flow.setFromStatus(fromStatus);
        flow.setToStatus(toStatus);
        flow.setOperateTime(operateTime);
        flow.setOperatorId(operatorId == null ? 0L : operatorId);
        flow.setSource(source);
        flow.setRemark(remark);
        flow.setCreatedAt(now());
        flowMapper.insert(flow);
    }

    private ProblemTicket requireActiveTicket(Long ticketId) {
        ProblemTicket ticket = problemTicketMapper.selectActiveById(ticketId);
        if (ticket == null) {
            throw new BusinessException("ticket not found: " + ticketId);
        }
        return ticket;
    }

    private ProblemTicket requireActiveTicketForUpdate(Long ticketId) {
        ProblemTicket ticket = problemTicketMapper.selectActiveByIdForUpdate(ticketId);
        if (ticket == null) {
            throw new BusinessException("ticket not found: " + ticketId);
        }
        return ticket;
    }

    private void validateTransition(IssueStatus currentStatus, IssueStatus targetStatus) {
        Set<IssueStatus> allowedTargets = ALLOWED_TRANSITIONS.get(currentStatus);
        if (allowedTargets == null || !allowedTargets.contains(targetStatus)) {
            throw new BusinessException("invalid status transition: " + currentStatus + " -> " + targetStatus);
        }
    }

    private ProblemTicketResponse toResponse(ProblemTicket ticket) {
        return new ProblemTicketResponse(
            ticket.getId(),
            ticket.getTicketNo(),
            ticket.getDescription(),
            ticket.getSubmitTime(),
            ticket.getStatus(),
            ticket.getStatusChangedAt()
        );
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
