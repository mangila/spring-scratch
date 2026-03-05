package com.github.mangila.app.movie.scheduler.outbox.relay.step;

import com.github.mangila.app.shared.persistence.base.projection.OutboxProjection;
import org.jobrunr.jobs.context.JobContext;

import java.util.List;

public record ClaimBatchStepResult(List<OutboxProjection> outboxProjections) implements JobContext.StepResult {

}
