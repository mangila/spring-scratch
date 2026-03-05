package com.github.mangila.app.movie.scheduler.outbox.purge;

import com.github.mangila.app.movie.service.MovieOutboxService;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.springframework.stereotype.Component;

@Component
public class MovieOutboxPurgeJobHandler implements JobRequestHandler<MovieOutboxPurgeJobRequest> {

    private final MovieOutboxService movieOutboxService;

    public MovieOutboxPurgeJobHandler(MovieOutboxService movieOutboxService) {
        this.movieOutboxService = movieOutboxService;
    }

    @Override
    public void run(MovieOutboxPurgeJobRequest jobRequest) throws Exception {
    }
}
