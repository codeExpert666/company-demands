package com.example.betademands.mapper;

import com.example.betademands.entity.ProblemTicketStatusFlow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProblemTicketStatusFlowMapper {

    int insert(ProblemTicketStatusFlow flow);

    int deleteByTicketIds(@Param("ids") List<Long> ids);

    List<ProblemTicketStatusFlow> selectByTicketId(@Param("ticketId") Long ticketId);
}
