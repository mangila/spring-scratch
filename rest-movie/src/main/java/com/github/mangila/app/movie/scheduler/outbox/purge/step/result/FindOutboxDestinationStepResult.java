package com.github.mangila.app.movie.scheduler.outbox.purge.step.result;

import com.github.mangila.app.movie.persistance.outbox.destination.MovieOutboxDestinationEntity;
import org.jobrunr.jobs.context.JobContext;

import java.util.List;

public record FindOutboxDestinationStepResult(
        List<MovieOutboxDestinationEntity> result) implements JobContext.StepResult {
}
