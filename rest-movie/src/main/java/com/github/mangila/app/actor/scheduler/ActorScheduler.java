package com.github.mangila.app.actor.scheduler;

import com.github.mangila.app.actor.properties.ActorProperties;
import com.github.mangila.app.actor.scheduler.outbox.relay.ActorOutboxRelayJobRequest;
import org.intellij.lang.annotations.Language;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.RecurringJobBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class ActorScheduler {

	private static final Logger log = LoggerFactory.getLogger(ActorScheduler.class);

	private final JobRequestScheduler jobRequestScheduler;

	private final ActorProperties actorProperties;

	public ActorScheduler(JobRequestScheduler jobRequestScheduler, ActorProperties actorProperties) {
		this.jobRequestScheduler = jobRequestScheduler;
		this.actorProperties = actorProperties;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onReady() {
		final var outbox = actorProperties.getOutbox();
		if (outbox.isEnabled()) {
			log.info("Actor outbox enabled");
			var jobRequest = new ActorOutboxRelayJobRequest(outbox.getLimit());
			var id = schedule(outbox.getCron(), jobRequest);
			log.info("Actor outbox relay scheduled with id: {}", id);
		}
	}

	public String schedule(@Language("CronExp") String cron, ActorOutboxRelayJobRequest request) {
		var job = RecurringJobBuilder.aRecurringJob()
			.withCron(cron)
			.withName("Actor outbox relay")
			.withJobRequest(request)
			.withLabels("actor", "outbox")
			.withAmountOfRetries(10);
		return jobRequestScheduler.createRecurrently(job);
	}

}
