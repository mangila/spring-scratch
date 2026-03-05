package com.github.mangila.app.movie.scheduler.outbox.process.step;

import com.github.mangila.app.movie.scheduler.outbox.process.step.result.FetchPayloadStepResult;
import com.github.mangila.app.movie.service.MovieHistoryService;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FetchPayloadStepHandler {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(FetchPayloadStepHandler.class));

    private final MovieHistoryService movieHistoryService;

    public FetchPayloadStepHandler(MovieHistoryService movieHistoryService) {
        this.movieHistoryService = movieHistoryService;
    }

    @Retryable
    public FetchPayloadStepResult handle(UUID historyId) {
        try {
            var payload = movieHistoryService.findPayloadById(historyId);
            return new FetchPayloadStepResult(payload);
        } catch (Exception e) {
            log.error("Error while fetching payload for history: {} - {}", historyId, e.getMessage(), e);
            throw e;
        }
    }

}
