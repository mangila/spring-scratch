package com.github.mangila.app.movie.scheduler.outbox.consumer.step;

import com.github.mangila.app.shared.persistence.base.projection.OutboxDestinationProjection;
import org.jobrunr.jobs.context.JobContext;

import java.util.List;

public record ClaimStep(List<OutboxDestinationProjection> destinationProjections) implements JobContext.StepResult {
}
