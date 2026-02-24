package io.github.mangila.spring1.pokemon.internal.scheduler;

import java.util.stream.Stream;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.springframework.stereotype.Service;

@Service
public class JobRunrScheduler {

  private final JobRequestScheduler jobRequestScheduler;

  public JobRunrScheduler(JobRequestScheduler jobRequestScheduler) {
    this.jobRequestScheduler = jobRequestScheduler;
  }

  public void enqueue(Stream<? extends JobRequest> jobRequests) {
    jobRequestScheduler.enqueue(jobRequests);
  }
}
