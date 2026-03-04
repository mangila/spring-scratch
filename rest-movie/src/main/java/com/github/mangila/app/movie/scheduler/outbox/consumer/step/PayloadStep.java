package com.github.mangila.app.movie.scheduler.outbox.consumer.step;

import com.github.mangila.app.shared.persistence.base.projection.HistoryPayloadProjection;
import org.jobrunr.jobs.context.JobContext;

public record PayloadStep(HistoryPayloadProjection projection) implements JobContext.StepResult {
}
