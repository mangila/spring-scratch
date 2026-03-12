package com.github.mangila.app.actor.outbox.recover;

import com.github.mangila.app.actor.outbox.recover.process.ActorOutboxRecoverPurgeProcessor;
import com.github.mangila.app.actor.outbox.recover.process.ActorOutboxRecoverRelayProcessor;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.navigation.AmountRequest;
import org.jobrunr.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ActorOutboxRecoverJobHandler implements JobRequestHandler<ActorOutboxRecoverJobRequest> {

	private static final Logger log = new JobRunrDashboardLogger(
			LoggerFactory.getLogger(ActorOutboxRecoverJobHandler.class));

	private final ActorOutboxRecoverRelayProcessor actorOutboxRecoverRelayProcessor;

	private final ActorOutboxRecoverPurgeProcessor actorOutboxRecoverPurgeProcessor;

	private final StorageProvider storageProvider;

	public ActorOutboxRecoverJobHandler(ActorOutboxRecoverRelayProcessor actorOutboxRecoverRelayProcessor,
			ActorOutboxRecoverPurgeProcessor actorOutboxRecoverPurgeProcessor, StorageProvider storageProvider) {
		this.actorOutboxRecoverRelayProcessor = actorOutboxRecoverRelayProcessor;
		this.actorOutboxRecoverPurgeProcessor = actorOutboxRecoverPurgeProcessor;
		this.storageProvider = storageProvider;
	}

	@Override
	public void run(ActorOutboxRecoverJobRequest jobRequest) throws Exception {
		final var context = ThreadLocalJobContext.getJobContext();
		final var limit = jobRequest.limit();

		var jobs = storageProvider.getJobList(StateName.FAILED, new AmountRequest("", limit));

		for (var job : jobs) {
			log.info("Job: {}", job.getId());

			var labels = job.getLabels();

			if (CollectionUtils.isNullOrEmpty(labels)) {
				log.info("No labels found for job");
				continue;
			}

			var isActorDomain = matchLabel(labels, "actor");
			var isOutbox = matchLabel(labels, "outbox");

			if (isActorDomain && isOutbox) {
				var isRelay = matchLabel(labels, "relay");
				if (isRelay) {
					actorOutboxRecoverRelayProcessor.process(job);
				}
				var isPurge = matchLabel(labels, "purge");
				if (isPurge) {
					actorOutboxRecoverPurgeProcessor.process(job);
				}
			}
		}
	}

	public static boolean matchLabel(List<String> labels, String label) {
		return labels.stream().anyMatch(l -> l.equals(label));
	}

}
