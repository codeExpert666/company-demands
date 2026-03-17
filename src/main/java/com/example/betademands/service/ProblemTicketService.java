package com.example.betademands.service;

import com.example.betademands.dto.ChangeProblemTicketStatusRequest;
import com.example.betademands.dto.CreateProblemTicketRequest;
import com.example.betademands.dto.ProblemTicketDashboardMetricsResponse;
import com.example.betademands.dto.ProblemTicketResponse;
import com.example.betademands.dto.StatusOptionResponse;
import com.example.betademands.dto.UpdateProblemTicketRequest;
import com.example.betademands.entity.enums.FlowSource;

import java.util.List;

public interface ProblemTicketService {

    ProblemTicketResponse createTicket(CreateProblemTicketRequest request, Long operatorId, FlowSource source);

    List<ProblemTicketResponse> importTickets(List<CreateProblemTicketRequest> requests, Long operatorId);

    ProblemTicketResponse updateTicket(Long ticketId, UpdateProblemTicketRequest request, Long operatorId);

    ProblemTicketResponse changeStatus(Long ticketId, ChangeProblemTicketStatusRequest request, Long operatorId);

    void deleteTickets(List<Long> ids);

    ProblemTicketDashboardMetricsResponse getDashboardMetrics();

    List<StatusOptionResponse> getAllStatusOptions();

    List<StatusOptionResponse> getNextStatusOptions(Long ticketId);
}
