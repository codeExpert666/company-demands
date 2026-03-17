package com.example.betademands.mapper;

import com.example.betademands.entity.DashboardMetricsAggregate;
import com.example.betademands.entity.ProblemTicketMetric;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ProblemTicketMetricMapper {

    int insert(ProblemTicketMetric metric);

    ProblemTicketMetric selectByTicketId(@Param("ticketId") Long ticketId);

    ProblemTicketMetric selectByTicketIdForUpdate(@Param("ticketId") Long ticketId);

    int update(ProblemTicketMetric metric);

    int deleteByTicketIds(@Param("ids") List<Long> ids);

    DashboardMetricsAggregate aggregateDashboardMetrics(@Param("now") LocalDateTime now);
}
