package com.github.mangila.app.movie.scheduler.outbox.shared.result;

import com.github.mangila.app.shared.persistence.base.projection.OutboxProjection;
import org.jobrunr.jobs.context.JobContext;

import java.util.List;

public record ClaimBatchStepResult(List<OutboxProjection> result) implements JobContext.StepResult {

}
