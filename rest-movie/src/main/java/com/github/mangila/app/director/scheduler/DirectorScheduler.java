package com.github.mangila.app.director.scheduler;

import com.github.mangila.app.director.properties.DirectorProperties;
import com.github.mangila.app.director.scheduler.outbox.relay.DirectorOutboxRelayJobRequest;
import org.intellij.lang.annotations.Language;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.RecurringJobBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class DirectorScheduler {

	private static final Logger log = LoggerFactory.getLogger(DirectorScheduler.class);

	private final DirectorProperties directorProperties;

	private final JobRequestScheduler jobRequestScheduler;

	public DirectorScheduler(DirectorProperties directorProperties, JobRequestScheduler jobRequestScheduler) {
		this.directorProperties = directorProperties;
		this.jobRequestScheduler = jobRequestScheduler;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onReady() {
		final var outbox = directorProperties.getOutbox();
		if (outbox.isEnabled()) {
			log.info("Director outbox enabled");
			var jobRequest = new DirectorOutboxRelayJobRequest(outbox.getLimit());
			var id = schedule(outbox.getCron(), jobRequest);
			log.info("Director outbox relay scheduled with id: {}", id);
		}
	}

	public String schedule(@Language("CronExp") String cron, DirectorOutboxRelayJobRequest request) {
		var job = RecurringJobBuilder.aRecurringJob()
			.withCron(cron)
			.withName("Director outbox relay")
			.withJobRequest(request)
			.withLabels("director", "outbox")
			.withAmountOfRetries(10);
		return jobRequestScheduler.createRecurrently(job);
	}

}
