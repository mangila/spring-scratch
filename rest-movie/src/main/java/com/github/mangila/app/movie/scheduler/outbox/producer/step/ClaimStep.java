package com.github.mangila.app.movie.scheduler.outbox.producer.step;

import com.github.mangila.app.shared.persistence.base.projection.OutboxProjection;
import org.jobrunr.jobs.context.JobContext;

import java.util.List;

public record ClaimStep(List<OutboxProjection> outboxProjections) implements JobContext.StepResult {

}
