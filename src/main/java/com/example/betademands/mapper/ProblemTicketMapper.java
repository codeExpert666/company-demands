package com.example.betademands.mapper;

import com.example.betademands.entity.ProblemTicket;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ProblemTicketMapper {

    int insert(ProblemTicket ticket);

    ProblemTicket selectActiveById(@Param("id") Long id);

    ProblemTicket selectActiveByIdForUpdate(@Param("id") Long id);

    int updateSelective(ProblemTicket ticket);

    int deleteByIds(@Param("ids") List<Long> ids);

    List<ProblemTicket> selectAllActive();

    long countActiveByIds(@Param("ids") List<Long> ids);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("statusChangedAt") LocalDateTime statusChangedAt,
                     @Param("updatedAt") LocalDateTime updatedAt);
}
