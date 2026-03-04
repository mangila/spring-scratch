package com.github.mangila.app.movie.scheduler;

import com.github.mangila.app.movie.properties.MovieProperties;
import com.github.mangila.app.movie.scheduler.outbox.consumer.MovieOutboxConsumeRelayJobRequest;
import com.github.mangila.app.movie.scheduler.outbox.consumer.destination.http.MovieHttpDestinationJobRequest;
import com.github.mangila.app.movie.scheduler.outbox.consumer.destination.kafka.MovieKafkaDestinationJobRequest;
import com.github.mangila.app.movie.scheduler.outbox.producer.MovieOutboxProduceJobRequest;
import com.github.mangila.app.movie.scheduler.outbox.producer.MovieOutboxProduceRelayJobRequest;
import com.github.mangila.app.shared.chaos.Chaos;
import org.intellij.lang.annotations.Language;
import org.jobrunr.jobs.JobId;
import org.jobrunr.scheduling.JobBuilder;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.RecurringJobBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class MovieScheduler implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(MovieScheduler.class);

	private final JobRequestScheduler jobRequestScheduler;

	private final MovieProperties movieProperties;

	public MovieScheduler(JobRequestScheduler jobRequestScheduler, MovieProperties movieProperties) {
		this.jobRequestScheduler = jobRequestScheduler;
		this.movieProperties = movieProperties;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		final var outbox = movieProperties.getOutbox();
		if (outbox.isEnabled()) {
			var produceRelayJobRequest = new MovieOutboxProduceRelayJobRequest(outbox.getLimit());
			var id = schedule(outbox.getCron(), produceRelayJobRequest);
			log.info("Movie outbox produce relay recurring job scheduled: {}", id);
			var consumeRelayJobRequest = new MovieOutboxConsumeRelayJobRequest(outbox.getLimit());
			id = schedule(outbox.getCron(), consumeRelayJobRequest);
			log.info("Movie outbox consume relay recurring job scheduled: {}", id);
		}
	}

	public String schedule(@Language("CronExp") String cron, MovieOutboxProduceRelayJobRequest request) {
		var job = RecurringJobBuilder.aRecurringJob()
			.withCron(cron)
			.withName("Movie outbox produce relay")
			.withJobRequest(request)
			.withLabels("movie", "outbox", "produce")
			.withAmountOfRetries(10);
		return jobRequestScheduler.createRecurrently(job);
	}

	public String schedule(@Language("CronExp") String cron, MovieOutboxConsumeRelayJobRequest request) {
		var job = RecurringJobBuilder.aRecurringJob()
			.withCron(cron)
			.withName("Movie outbox consume relay")
			.withJobRequest(request)
			.withLabels("movie", "outbox", "consume")
			.withAmountOfRetries(10);
		return jobRequestScheduler.createRecurrently(job);
	}

	@Chaos
	public JobId schedule(MovieOutboxProduceJobRequest request) {
		var job = JobBuilder.aJob()
			.scheduleIn(Duration.ofSeconds(1))
			.withName("Movie outbox produce: %s".formatted(request.outbox().id()))
			.withJobRequest(request)
			.withLabels("movie", "outbox", "produce")
			.withAmountOfRetries(10);
		return jobRequestScheduler.create(job);
	}

	@Chaos
	public JobId schedule(MovieHttpDestinationJobRequest request) {
		final var destinationId = request.destinationId();
		final var destination = request.destination();
		var job = JobBuilder.aJob()
			.scheduleIn(Duration.ofSeconds(1))
			.withName("Movie %s destination: %s".formatted(destination.toString(), destinationId))
			.withJobRequest(request)
			.withLabels("movie", "outbox", "destination")
			.withAmountOfRetries(10);
		return jobRequestScheduler.create(job);
	}

	@Chaos
	public JobId schedule(MovieKafkaDestinationJobRequest request) {
		final var destinationId = request.destinationId();
		final var destination = request.destination();
		var job = JobBuilder.aJob()
			.scheduleIn(Duration.ofSeconds(1))
			.withName("Movie %s destination: %s".formatted(destination.toString(), destinationId))
			.withJobRequest(request)
			.withLabels("movie", "outbox", "destination")
			.withAmountOfRetries(10);
		return jobRequestScheduler.create(job);
	}

}
