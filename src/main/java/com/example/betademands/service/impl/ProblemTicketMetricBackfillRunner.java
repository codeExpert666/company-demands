package com.example.betademands.service.impl;

import com.example.betademands.service.ProblemTicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ProblemTicketMetricBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProblemTicketMetricBackfillRunner.class);

    private final ProblemTicketService problemTicketService;

    public ProblemTicketMetricBackfillRunner(ProblemTicketService problemTicketService) {
        this.problemTicketService = problemTicketService;
    }

    @Override
    public void run(ApplicationArguments args) {
        int count = problemTicketService.backfillMissingMetrics();
        if (count > 0) {
            log.info("Backfilled {} problem ticket metric rows.", count);
        }
    }
}
