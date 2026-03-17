package com.example.betademands.service.impl;

import com.example.betademands.config.ProblemTicketMetricsProperties;
import com.example.betademands.dto.ChangeProblemTicketStatusRequest;
import com.example.betademands.dto.CreateProblemTicketRequest;
import com.example.betademands.dto.MetricCardResponse;
import com.example.betademands.dto.ProblemTicketDashboardMetricsResponse;
import com.example.betademands.dto.ProblemTicketResponse;
import com.example.betademands.dto.StatusOptionResponse;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProblemTicketServiceImpl implements ProblemTicketService {

    private static final Map<IssueStatus, Set<IssueStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(IssueStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(IssueStatus.ANALYZING, EnumSet.of(IssueStatus.FIXING, IssueStatus.CLOSED, IssueStatus.SUSPENDED));
        ALLOWED_TRANSITIONS.put(IssueStatus.FIXING, EnumSet.of(IssueStatus.PUSHING));
        ALLOWED_TRANSITIONS.put(IssueStatus.PUSHING, EnumSet.of(IssueStatus.CLOSED));
        ALLOWED_TRANSITIONS.put(IssueStatus.CLOSED, EnumSet.noneOf(IssueStatus.class));
        ALLOWED_TRANSITIONS.put(IssueStatus.SUSPENDED, EnumSet.of(IssueStatus.FIXING));
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
        LocalDateTime operationTime = now();
        LocalDateTime submitTime = request.submitTime() == null ? operationTime : request.submitTime();
        IssueStatus initialStatus = request.status() == null ? IssueStatus.ANALYZING : request.status();
        LocalDateTime statusChangedAt = initialStatus == IssueStatus.ANALYZING ? submitTime : operationTime;

        ProblemTicket ticket = new ProblemTicket();
        ticket.setTicketNo(request.ticketNo());
        ticket.setDescription(request.description());
        ticket.setSubmitTime(submitTime);
        ticket.setStatus(initialStatus);
        ticket.setStatusChangedAt(statusChangedAt);
        ticket.setDeleted(false);
        ticket.setCreatedAt(operationTime);
        ticket.setUpdatedAt(operationTime);
        problemTicketMapper.insert(ticket);

        insertFlow(ticket.getId(), null, initialStatus, statusChangedAt, operatorId, source, "initial status");
        metricMapper.insert(initMetric(ticket.getId(), submitTime, initialStatus, operationTime));

        return toResponse(problemTicketMapper.selectActiveById(ticket.getId()));
    }

    @Override
    @Transactional
    public List<ProblemTicketResponse> importTickets(List<CreateProblemTicketRequest> requests, Long operatorId) {
        if (CollectionUtils.isEmpty(requests)) {
            throw new BusinessException("import items cannot be empty");
        }
        List<ProblemTicketResponse> responses = new ArrayList<>(requests.size());
        for (CreateProblemTicketRequest request : requests) {
            ProblemTicketResponse response = createTicket(request, operatorId, FlowSource.IMPORT);
            responses.add(response);
        }
        return responses;
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
        DashboardMetricsAggregate aggregate = metricMapper.aggregateDashboardMetrics(now());
        return new ProblemTicketDashboardMetricsResponse(
            new MetricCardResponse(aggregate.getAnalysisAvgDurationSec(), aggregate.getAnalysisSampleCount()),
            new MetricCardResponse(aggregate.getModifyAvgDurationSec(), aggregate.getModifySampleCount()),
            new MetricCardResponse(aggregate.getPushAvgDurationSec(), aggregate.getPushSampleCount()),
            new MetricCardResponse(aggregate.getClosedLoopAvgDurationSec(), aggregate.getClosedLoopSampleCount()),
            metricsProperties.getAccurateFrom()
        );
    }

    @Override
    public List<StatusOptionResponse> getAllStatusOptions() {
        return Arrays.stream(IssueStatus.values())
            .map(this::toStatusOption)
            .collect(Collectors.toList());
    }

    @Override
    public List<StatusOptionResponse> getNextStatusOptions(Long ticketId) {
        ProblemTicket ticket = requireActiveTicket(ticketId);
        return ALLOWED_TRANSITIONS.get(ticket.getStatus()).stream()
            .map(this::toStatusOption)
            .collect(Collectors.toList());
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
        long segmentSec = calculateSegmentSeconds(metric.getCurrentStageEnteredAt(), operationTime);

        if (ticket.getStatus() == IssueStatus.ANALYZING && metric.isAnalysisTrackable()) {
            metric.setAnalysisDurationSec(metric.getAnalysisDurationSec() + segmentSec);
        } else if (ticket.getStatus() == IssueStatus.FIXING && metric.isModifyTrackable()) {
            metric.setModifyDurationSec(metric.getModifyDurationSec() + segmentSec);
        } else if (ticket.getStatus() == IssueStatus.PUSHING && metric.isPushTrackable()) {
            metric.setPushDurationSec(metric.getPushDurationSec() + segmentSec);
        }

        if (targetStatus == IssueStatus.FIXING) {
            metric.setModifyTrackable(true);
            metric.setCurrentStageEnteredAt(operationTime);
            metric.setClosedLoopDurationSec(null);
            metric.setClosedLoopCompleted(false);
        } else if (targetStatus == IssueStatus.PUSHING) {
            metric.setPushTrackable(true);
            metric.setCurrentStageEnteredAt(operationTime);
            metric.setClosedLoopDurationSec(null);
            metric.setClosedLoopCompleted(false);
        } else if (targetStatus == IssueStatus.SUSPENDED) {
            metric.setCurrentStageEnteredAt(null);
            metric.setClosedLoopDurationSec(null);
            metric.setClosedLoopCompleted(false);
        } else if (targetStatus == IssueStatus.CLOSED) {
            metric.setClosedLoopDurationSec(ChronoUnit.SECONDS.between(metric.getSubmitTime(), operationTime));
            metric.setClosedLoopCompleted(true);
            metric.setCurrentStageEnteredAt(null);
        }

        metric.setCurrentStatus(targetStatus);
        metric.setUpdatedAt(operationTime);
        metricMapper.update(metric);

        problemTicketMapper.updateStatus(ticketId, targetStatus.name(), operationTime, operationTime);
        insertFlow(ticketId, ticket.getStatus(), targetStatus, operationTime, operatorId, FlowSource.STATUS_UPDATE, remark);
    }

    private ProblemTicketMetric initMetric(Long ticketId, LocalDateTime submitTime, IssueStatus currentStatus, LocalDateTime operationTime) {
        ProblemTicketMetric metric = new ProblemTicketMetric();
        metric.setTicketId(ticketId);
        metric.setSubmitTime(submitTime);
        metric.setCurrentStatus(currentStatus);
        metric.setAnalysisDurationSec(0);
        metric.setAnalysisTrackable(false);
        metric.setModifyDurationSec(0);
        metric.setModifyTrackable(false);
        metric.setPushDurationSec(0);
        metric.setPushTrackable(false);
        metric.setClosedLoopDurationSec(null);
        metric.setClosedLoopCompleted(false);
        metric.setCurrentStageEnteredAt(null);
        metric.setUpdatedAt(operationTime);

        if (currentStatus == IssueStatus.ANALYZING) {
            metric.setAnalysisTrackable(true);
            metric.setCurrentStageEnteredAt(submitTime);
        }

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

    private long calculateSegmentSeconds(LocalDateTime stageEnteredAt, LocalDateTime operationTime) {
        if (stageEnteredAt == null) {
            return 0;
        }
        long segmentSec = ChronoUnit.SECONDS.between(stageEnteredAt, operationTime);
        if (segmentSec < 0) {
            throw new BusinessException("status change time is earlier than current stage start time");
        }
        return segmentSec;
    }

    private StatusOptionResponse toStatusOption(IssueStatus status) {
        return new StatusOptionResponse(status.name(), status.getLabel());
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
