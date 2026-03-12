package com.github.mangila.app.director.outbox.recover;

import com.github.mangila.app.director.outbox.recover.process.DirectorOutboxRecoverPurgeProcessor;
import com.github.mangila.app.director.outbox.recover.process.DirectorOutboxRecoverRelayProcessor;
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
public class DirectorOutboxRecoverJobHandler implements JobRequestHandler<DirectorOutboxRecoverJobRequest> {

	private static final Logger log = new JobRunrDashboardLogger(
			LoggerFactory.getLogger(DirectorOutboxRecoverJobHandler.class));

	private final DirectorOutboxRecoverRelayProcessor directorOutboxRecoverRelayProcessor;

	private final DirectorOutboxRecoverPurgeProcessor directorOutboxRecoverPurgeProcessor;

	private final StorageProvider storageProvider;

	public DirectorOutboxRecoverJobHandler(DirectorOutboxRecoverRelayProcessor directorOutboxRecoverRelayProcessor,
			DirectorOutboxRecoverPurgeProcessor directorOutboxRecoverPurgeProcessor, StorageProvider storageProvider) {
		this.directorOutboxRecoverRelayProcessor = directorOutboxRecoverRelayProcessor;
		this.directorOutboxRecoverPurgeProcessor = directorOutboxRecoverPurgeProcessor;
		this.storageProvider = storageProvider;
	}

	@Override
	public void run(DirectorOutboxRecoverJobRequest jobRequest) throws Exception {
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

			var isDirectorDomain = matchLabel(labels, "director");
			var isOutbox = matchLabel(labels, "outbox");

			if (isDirectorDomain && isOutbox) {
				var isRelay = matchLabel(labels, "relay");
				if (isRelay) {
					directorOutboxRecoverRelayProcessor.process(job);
				}
				var isPurge = matchLabel(labels, "purge");
				if (isPurge) {
					directorOutboxRecoverPurgeProcessor.process(job);
				}
			}
		}
	}

	public static boolean matchLabel(List<String> labels, String label) {
		return labels.stream().anyMatch(l -> l.equals(label));
	}

}
