package com.example.betademands.controller;

import com.example.betademands.dto.ChangeProblemTicketStatusRequest;
import com.example.betademands.dto.CreateProblemTicketRequest;
import com.example.betademands.dto.DeleteProblemTicketsRequest;
import com.example.betademands.dto.ImportProblemTicketsRequest;
import com.example.betademands.dto.ProblemTicketDashboardMetricsResponse;
import com.example.betademands.dto.ProblemTicketResponse;
import com.example.betademands.dto.UpdateProblemTicketRequest;
import com.example.betademands.entity.enums.FlowSource;
import com.example.betademands.service.ProblemTicketService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/problem-tickets")
public class ProblemTicketController {

    private final ProblemTicketService problemTicketService;

    public ProblemTicketController(ProblemTicketService problemTicketService) {
        this.problemTicketService = problemTicketService;
    }

    @PostMapping
    public ProblemTicketResponse createTicket(@Valid @RequestBody CreateProblemTicketRequest request,
                                              @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId) {
        return problemTicketService.createTicket(request, operatorId, FlowSource.CREATE);
    }

    @PostMapping("/import")
    public List<ProblemTicketResponse> importTickets(@Valid @RequestBody ImportProblemTicketsRequest request,
                                                     @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId) {
        return problemTicketService.importTickets(request.items(), operatorId);
    }

    @PutMapping("/{ticketId}")
    public ProblemTicketResponse updateTicket(@PathVariable Long ticketId,
                                              @RequestBody UpdateProblemTicketRequest request,
                                              @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId) {
        return problemTicketService.updateTicket(ticketId, request, operatorId);
    }

    @PutMapping("/{ticketId}/status")
    public ProblemTicketResponse changeStatus(@PathVariable Long ticketId,
                                              @Valid @RequestBody ChangeProblemTicketStatusRequest request,
                                              @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId) {
        return problemTicketService.changeStatus(ticketId, request, operatorId);
    }

    @DeleteMapping
    public void deleteTickets(@Valid @RequestBody DeleteProblemTicketsRequest request) {
        problemTicketService.deleteTickets(request.ids());
    }

    @GetMapping("/dashboard/metrics")
    public ProblemTicketDashboardMetricsResponse getDashboardMetrics() {
        return problemTicketService.getDashboardMetrics();
    }
}
