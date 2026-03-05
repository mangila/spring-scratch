package com.github.mangila.app.movie.scheduler.outbox.relay.step;

import com.github.mangila.app.shared.persistence.base.projection.HistoryPayloadProjection;
import org.jobrunr.jobs.context.JobContext;

public record FetchPayloadStepResult(HistoryPayloadProjection projection) implements JobContext.StepResult {
}
