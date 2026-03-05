package com.github.mangila.app.movie.scheduler.outbox.relay.step;

import com.github.mangila.app.movie.persistance.outbox.destination.MovieOutboxDestinationEntity;
import org.jobrunr.jobs.context.JobContext;

import java.util.List;

public record CreateDestinationStepResult(List<MovieOutboxDestinationEntity> destinationEntities) implements JobContext.StepResult {
}
