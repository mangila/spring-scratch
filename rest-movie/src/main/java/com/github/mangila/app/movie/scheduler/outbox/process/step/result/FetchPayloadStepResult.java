package com.github.mangila.app.movie.scheduler.outbox.process.step.result;

import com.github.mangila.app.shared.persistence.base.projection.HistoryPayloadProjection;
import org.jobrunr.jobs.context.JobContext;

public record FetchPayloadStepResult(HistoryPayloadProjection result) implements JobContext.StepResult {
}
