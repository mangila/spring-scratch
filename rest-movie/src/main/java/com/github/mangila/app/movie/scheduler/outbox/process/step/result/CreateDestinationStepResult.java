package com.github.mangila.app.movie.scheduler.outbox.process.step.result;

import com.github.mangila.app.movie.persistance.outbox.destination.MovieOutboxDestinationEntity;
import org.jobrunr.jobs.context.JobContext;

import java.util.List;

public record CreateDestinationStepResult(
		List<MovieOutboxDestinationEntity> result) implements JobContext.StepResult {
}
