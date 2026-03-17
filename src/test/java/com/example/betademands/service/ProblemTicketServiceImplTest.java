package com.example.betademands.service;

import com.example.betademands.config.ProblemTicketMetricsProperties;
import com.example.betademands.dto.ChangeProblemTicketStatusRequest;
import com.example.betademands.dto.CreateProblemTicketRequest;
import com.example.betademands.dto.ProblemTicketDashboardMetricsResponse;
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
import com.example.betademands.service.impl.ProblemTicketServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProblemTicketServiceImplTest {

    @Mock
    private ProblemTicketMapper problemTicketMapper;
    @Mock
    private ProblemTicketStatusFlowMapper flowMapper;
    @Mock
    private ProblemTicketMetricMapper metricMapper;

    private MutableClock clock;
    private ProblemTicketServiceImpl service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(LocalDateTime.of(2026, 3, 15, 9, 0));
        ProblemTicketMetricsProperties properties = new ProblemTicketMetricsProperties();
        properties.setAccurateFrom(LocalDateTime.of(2026, 3, 15, 0, 0));
        service = new ProblemTicketServiceImpl(problemTicketMapper, flowMapper, metricMapper, properties, clock);
    }

    @Test
    void createTicket_shouldDefaultToAnalyzingAndInitializeTrackableMetric() {
        AtomicReference<ProblemTicket> storedTicket = new AtomicReference<>();
        when(flowMapper.insert(any())).thenReturn(1);
        when(metricMapper.insert(any())).thenReturn(1);
        doAnswer(invocation -> {
            ProblemTicket ticket = invocation.getArgument(0);
            ticket.setId(1L);
            storedTicket.set(ticket);
            return 1;
        }).when(problemTicketMapper).insert(any(ProblemTicket.class));
        when(problemTicketMapper.selectActiveById(1L)).thenAnswer(invocation -> storedTicket.get());

        LocalDateTime submitTime = LocalDateTime.of(2026, 3, 15, 8, 30);
        service.createTicket(new CreateProblemTicketRequest("BETA-1", "first issue", submitTime, null), 99L, FlowSource.CREATE);

        ArgumentCaptor<ProblemTicketStatusFlow> flowCaptor = ArgumentCaptor.forClass(ProblemTicketStatusFlow.class);
        ArgumentCaptor<ProblemTicketMetric> metricCaptor = ArgumentCaptor.forClass(ProblemTicketMetric.class);
        verify(flowMapper).insert(flowCaptor.capture());
        verify(metricMapper).insert(metricCaptor.capture());

        ProblemTicketStatusFlow flow = flowCaptor.getValue();
        assertNull(flow.getFromStatus());
        assertEquals(IssueStatus.ANALYZING, flow.getToStatus());
        assertEquals(submitTime, flow.getOperateTime());
        assertEquals(99L, flow.getOperatorId());

        ProblemTicketMetric metric = metricCaptor.getValue();
        assertEquals(1L, metric.getTicketId());
        assertEquals(submitTime, metric.getSubmitTime());
        assertEquals(IssueStatus.ANALYZING, metric.getCurrentStatus());
        assertEquals(submitTime, metric.getCurrentStageEnteredAt());
        assertTrue(metric.isAnalysisTrackable());
        assertFalse(metric.isModifyTrackable());
        assertFalse(metric.isPushTrackable());
        assertFalse(metric.isClosedLoopCompleted());
    }

    @Test
    void createTicket_shouldAllowInitialFixingButKeepStageUntrackable() {
        AtomicReference<ProblemTicket> storedTicket = new AtomicReference<>();
        when(flowMapper.insert(any())).thenReturn(1);
        when(metricMapper.insert(any())).thenReturn(1);
        doAnswer(invocation -> {
            ProblemTicket ticket = invocation.getArgument(0);
            ticket.setId(2L);
            storedTicket.set(ticket);
            return 1;
        }).when(problemTicketMapper).insert(any(ProblemTicket.class));
        when(problemTicketMapper.selectActiveById(2L)).thenAnswer(invocation -> storedTicket.get());

        LocalDateTime submitTime = LocalDateTime.of(2026, 3, 15, 8, 0);
        service.createTicket(new CreateProblemTicketRequest("BETA-2", "already fixing", submitTime, IssueStatus.FIXING), 101L, FlowSource.IMPORT);

        ArgumentCaptor<ProblemTicketMetric> metricCaptor = ArgumentCaptor.forClass(ProblemTicketMetric.class);
        verify(metricMapper).insert(metricCaptor.capture());

        ProblemTicketMetric metric = metricCaptor.getValue();
        assertEquals(IssueStatus.FIXING, metric.getCurrentStatus());
        assertNull(metric.getCurrentStageEnteredAt());
        assertFalse(metric.isAnalysisTrackable());
        assertFalse(metric.isModifyTrackable());
        assertFalse(metric.isPushTrackable());
        assertFalse(metric.isClosedLoopCompleted());
    }

    @Test
    void changeStatus_shouldAccumulateClosedLoopMetricsUnderNewStateMachine() {
        ProblemTicket ticket = ticket(3L, "BETA-3", IssueStatus.ANALYZING,
            LocalDateTime.of(2026, 3, 15, 9, 0),
            LocalDateTime.of(2026, 3, 15, 9, 0));
        ProblemTicketMetric metric = metric(3L,
            LocalDateTime.of(2026, 3, 15, 9, 0),
            IssueStatus.ANALYZING,
            LocalDateTime.of(2026, 3, 15, 9, 0),
            true,
            false,
            false);
        stubMutableState(ticket, metric);

        clock.set(LocalDateTime.of(2026, 3, 15, 10, 0));
        service.changeStatus(3L, new ChangeProblemTicketStatusRequest(IssueStatus.FIXING, "start fix"), 7L);

        assertEquals(IssueStatus.FIXING, ticket.getStatus());
        assertEquals(3600L, metric.getAnalysisDurationSec());
        assertTrue(metric.isModifyTrackable());
        assertEquals(LocalDateTime.of(2026, 3, 15, 10, 0), metric.getCurrentStageEnteredAt());
        assertFalse(metric.isClosedLoopCompleted());

        clock.set(LocalDateTime.of(2026, 3, 15, 12, 0));
        service.changeStatus(3L, new ChangeProblemTicketStatusRequest(IssueStatus.PUSHING, "push version"), 7L);

        assertEquals(IssueStatus.PUSHING, ticket.getStatus());
        assertEquals(7200L, metric.getModifyDurationSec());
        assertTrue(metric.isPushTrackable());
        assertEquals(LocalDateTime.of(2026, 3, 15, 12, 0), metric.getCurrentStageEnteredAt());
        assertEquals(0L, metric.getPushDurationSec());

        clock.set(LocalDateTime.of(2026, 3, 15, 13, 0));
        service.changeStatus(3L, new ChangeProblemTicketStatusRequest(IssueStatus.CLOSED, "done"), 7L);

        assertEquals(IssueStatus.CLOSED, ticket.getStatus());
        assertEquals(3600L, metric.getAnalysisDurationSec());
        assertEquals(7200L, metric.getModifyDurationSec());
        assertEquals(3600L, metric.getPushDurationSec());
        assertEquals(14400L, metric.getClosedLoopDurationSec());
        assertTrue(metric.isClosedLoopCompleted());
        assertNull(metric.getCurrentStageEnteredAt());
        verify(flowMapper, times(3)).insert(any(ProblemTicketStatusFlow.class));
    }

    @Test
    void changeStatus_shouldHandleSuspendResumeAndExcludeSuspendedAsClosedLoopEnd() {
        ProblemTicket ticket = ticket(4L, "BETA-4", IssueStatus.ANALYZING,
            LocalDateTime.of(2026, 3, 15, 9, 0),
            LocalDateTime.of(2026, 3, 15, 9, 0));
        ProblemTicketMetric metric = metric(4L,
            LocalDateTime.of(2026, 3, 15, 9, 0),
            IssueStatus.ANALYZING,
            LocalDateTime.of(2026, 3, 15, 9, 0),
            true,
            false,
            false);
        stubMutableState(ticket, metric);

        clock.set(LocalDateTime.of(2026, 3, 15, 10, 0));
        service.changeStatus(4L, new ChangeProblemTicketStatusRequest(IssueStatus.SUSPENDED, "cannot solve now"), 3L);

        assertEquals(IssueStatus.SUSPENDED, ticket.getStatus());
        assertEquals(3600L, metric.getAnalysisDurationSec());
        assertNull(metric.getCurrentStageEnteredAt());
        assertFalse(metric.isClosedLoopCompleted());
        assertNull(metric.getClosedLoopDurationSec());

        clock.set(LocalDateTime.of(2026, 3, 15, 12, 0));
        service.changeStatus(4L, new ChangeProblemTicketStatusRequest(IssueStatus.FIXING, "resume"), 3L);

        assertEquals(IssueStatus.FIXING, ticket.getStatus());
        assertTrue(metric.isModifyTrackable());
        assertEquals(LocalDateTime.of(2026, 3, 15, 12, 0), metric.getCurrentStageEnteredAt());

        clock.set(LocalDateTime.of(2026, 3, 15, 14, 0));
        service.changeStatus(4L, new ChangeProblemTicketStatusRequest(IssueStatus.PUSHING, "push"), 3L);

        clock.set(LocalDateTime.of(2026, 3, 15, 15, 0));
        service.changeStatus(4L, new ChangeProblemTicketStatusRequest(IssueStatus.CLOSED, "close"), 3L);

        assertEquals(7200L, metric.getModifyDurationSec());
        assertEquals(3600L, metric.getPushDurationSec());
        assertEquals(21600L, metric.getClosedLoopDurationSec());
        assertTrue(metric.isClosedLoopCompleted());
    }

    @Test
    void changeStatus_shouldRejectIllegalTransition() {
        ProblemTicket ticket = ticket(5L, "BETA-5", IssueStatus.FIXING,
            LocalDateTime.of(2026, 3, 15, 9, 0),
            LocalDateTime.of(2026, 3, 15, 10, 0));
        when(problemTicketMapper.selectActiveByIdForUpdate(5L)).thenReturn(ticket);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> service.changeStatus(5L, new ChangeProblemTicketStatusRequest(IssueStatus.ANALYZING, "rollback"), 1L));

        assertEquals("invalid status transition: FIXING -> ANALYZING", exception.getMessage());
        verify(metricMapper, never()).selectByTicketIdForUpdate(any());
        verify(metricMapper, never()).update(any());
        verify(flowMapper, never()).insert(any());
    }

    @Test
    void updateTicket_withoutStatusChange_shouldNotWriteFlowOrMetric() {
        ProblemTicket ticket = ticket(6L, "BETA-6", IssueStatus.ANALYZING,
            LocalDateTime.of(2026, 3, 15, 9, 0),
            LocalDateTime.of(2026, 3, 15, 9, 0));
        when(problemTicketMapper.selectActiveById(6L)).thenReturn(ticket);
        doAnswer(invocation -> {
            ProblemTicket update = invocation.getArgument(0);
            if (update.getDescription() != null) {
                ticket.setDescription(update.getDescription());
            }
            ticket.setUpdatedAt(update.getUpdatedAt());
            return 1;
        }).when(problemTicketMapper).updateSelective(any(ProblemTicket.class));

        service.updateTicket(6L, new UpdateProblemTicketRequest(null, "new description", null, null), 8L);

        assertEquals("new description", ticket.getDescription());
        verify(problemTicketMapper).updateSelective(any(ProblemTicket.class));
        verify(flowMapper, never()).insert(any());
        verify(metricMapper, never()).update(any());
    }

    @Test
    void deleteTickets_shouldCascadeDeleteAuxiliaryTables() {
        when(problemTicketMapper.countActiveByIds(List.of(7L, 8L))).thenReturn(2L);

        service.deleteTickets(List.of(7L, 8L));

        verify(flowMapper).deleteByTicketIds(List.of(7L, 8L));
        verify(metricMapper).deleteByTicketIds(List.of(7L, 8L));
        verify(problemTicketMapper).deleteByIds(List.of(7L, 8L));
    }

    @Test
    void getStatusOptions_shouldReturnAllStatusesAndReachableNextStatuses() {
        ProblemTicket ticket = ticket(9L, "BETA-9", IssueStatus.ANALYZING,
            LocalDateTime.of(2026, 3, 15, 9, 0),
            LocalDateTime.of(2026, 3, 15, 9, 0));
        when(problemTicketMapper.selectActiveById(9L)).thenReturn(ticket);

        List<StatusOptionResponse> allOptions = service.getAllStatusOptions();
        List<StatusOptionResponse> nextOptions = service.getNextStatusOptions(9L);

        assertEquals(List.of("ANALYZING", "FIXING", "PUSHING", "CLOSED", "SUSPENDED"),
            allOptions.stream().map(StatusOptionResponse::code).toList());
        assertEquals(List.of("分析中", "修改中", "版本推送中", "问题关闭", "挂起"),
            allOptions.stream().map(StatusOptionResponse::label).toList());
        assertEquals(List.of("FIXING", "CLOSED", "SUSPENDED"),
            nextOptions.stream().map(StatusOptionResponse::code).toList());
    }

    @Test
    void getDashboardMetrics_shouldMapAggregate() {
        DashboardMetricsAggregate aggregate = new DashboardMetricsAggregate();
        aggregate.setAnalysisAvgDurationSec(3600L);
        aggregate.setAnalysisSampleCount(2L);
        aggregate.setModifyAvgDurationSec(5400L);
        aggregate.setModifySampleCount(1L);
        aggregate.setPushAvgDurationSec(1800L);
        aggregate.setPushSampleCount(1L);
        aggregate.setClosedLoopAvgDurationSec(7200L);
        aggregate.setClosedLoopSampleCount(3L);
        when(metricMapper.aggregateDashboardMetrics(any(LocalDateTime.class))).thenReturn(aggregate);

        ProblemTicketDashboardMetricsResponse response = service.getDashboardMetrics();

        assertEquals(3600L, response.analysis().avgDurationSec());
        assertEquals(2L, response.analysis().sampleCount());
        assertEquals(5400L, response.modify().avgDurationSec());
        assertEquals(1800L, response.push().avgDurationSec());
        assertEquals(7200L, response.closedLoop().avgDurationSec());
        assertEquals(3L, response.closedLoop().sampleCount());
        assertEquals(LocalDateTime.of(2026, 3, 15, 0, 0), response.accurateFrom());
    }

    private void stubMutableState(ProblemTicket ticket, ProblemTicketMetric metric) {
        when(problemTicketMapper.selectActiveByIdForUpdate(ticket.getId())).thenAnswer(invocation -> ticket);
        when(problemTicketMapper.selectActiveById(ticket.getId())).thenAnswer(invocation -> ticket);
        when(metricMapper.selectByTicketIdForUpdate(ticket.getId())).thenAnswer(invocation -> metric);
        doAnswer(invocation -> {
            ticket.setStatus(IssueStatus.from(invocation.getArgument(1)));
            ticket.setStatusChangedAt(invocation.getArgument(2));
            ticket.setUpdatedAt(invocation.getArgument(3));
            return 1;
        }).when(problemTicketMapper).updateStatus(eq(ticket.getId()), any(), any(), any());
        doAnswer(invocation -> {
            ProblemTicketMetric updated = invocation.getArgument(0);
            copyMetric(updated, metric);
            return 1;
        }).when(metricMapper).update(any(ProblemTicketMetric.class));
        when(flowMapper.insert(any())).thenReturn(1);
    }

    private void copyMetric(ProblemTicketMetric source, ProblemTicketMetric target) {
        target.setTicketId(source.getTicketId());
        target.setSubmitTime(source.getSubmitTime());
        target.setCurrentStatus(source.getCurrentStatus());
        target.setCurrentStageEnteredAt(source.getCurrentStageEnteredAt());
        target.setAnalysisDurationSec(source.getAnalysisDurationSec());
        target.setAnalysisTrackable(source.isAnalysisTrackable());
        target.setModifyDurationSec(source.getModifyDurationSec());
        target.setModifyTrackable(source.isModifyTrackable());
        target.setPushDurationSec(source.getPushDurationSec());
        target.setPushTrackable(source.isPushTrackable());
        target.setClosedLoopDurationSec(source.getClosedLoopDurationSec());
        target.setClosedLoopCompleted(source.isClosedLoopCompleted());
        target.setUpdatedAt(source.getUpdatedAt());
    }

    private ProblemTicket ticket(Long id, String ticketNo, IssueStatus status, LocalDateTime submitTime, LocalDateTime statusChangedAt) {
        ProblemTicket ticket = new ProblemTicket();
        ticket.setId(id);
        ticket.setTicketNo(ticketNo);
        ticket.setDescription("desc");
        ticket.setSubmitTime(submitTime);
        ticket.setStatus(status);
        ticket.setStatusChangedAt(statusChangedAt);
        ticket.setDeleted(false);
        ticket.setCreatedAt(submitTime);
        ticket.setUpdatedAt(statusChangedAt);
        return ticket;
    }

    private ProblemTicketMetric metric(Long ticketId,
                                       LocalDateTime submitTime,
                                       IssueStatus currentStatus,
                                       LocalDateTime currentStageEnteredAt,
                                       boolean analysisTrackable,
                                       boolean modifyTrackable,
                                       boolean pushTrackable) {
        ProblemTicketMetric metric = new ProblemTicketMetric();
        metric.setTicketId(ticketId);
        metric.setSubmitTime(submitTime);
        metric.setCurrentStatus(currentStatus);
        metric.setCurrentStageEnteredAt(currentStageEnteredAt);
        metric.setAnalysisDurationSec(0L);
        metric.setAnalysisTrackable(analysisTrackable);
        metric.setModifyDurationSec(0L);
        metric.setModifyTrackable(modifyTrackable);
        metric.setPushDurationSec(0L);
        metric.setPushTrackable(pushTrackable);
        metric.setClosedLoopDurationSec(null);
        metric.setClosedLoopCompleted(false);
        metric.setUpdatedAt(currentStageEnteredAt == null ? submitTime : currentStageEnteredAt);
        return metric;
    }

    private static class MutableClock extends Clock {

        private Instant instant;
        private final ZoneId zoneId = ZoneOffset.UTC;

        private MutableClock(LocalDateTime dateTime) {
            this.instant = dateTime.toInstant(ZoneOffset.UTC);
        }

        public void set(LocalDateTime dateTime) {
            this.instant = dateTime.toInstant(ZoneOffset.UTC);
        }

        @Override
        public ZoneId getZone() {
            return zoneId;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
