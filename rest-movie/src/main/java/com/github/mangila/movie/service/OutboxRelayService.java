package com.github.mangila.movie.service;

import com.github.mangila.movie.persistence.outbox.OutboxJdbcRepository;
import com.github.mangila.movie.persistence.outbox.version.OutboxVersionJpaRepository;
import com.github.mangila.movie.shared.VaadinEventPublisher;
import org.jobrunr.scheduling.JobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OutboxRelayService {

	private static final Logger log = LoggerFactory.getLogger(OutboxRelayService.class);

	private final JobScheduler jobScheduler;

	private final TransactionTemplate transactionTemplate;

	private final OutboxJdbcRepository outboxJdbcRepository;

	private final OutboxVersionJpaRepository outboxVersionJpaRepository;

	private final VaadinEventPublisher vaadinEventPublisher;

	public OutboxRelayService(JobScheduler jobScheduler, TransactionTemplate transactionTemplate,
			OutboxJdbcRepository outboxJdbcRepository, OutboxVersionJpaRepository outboxVersionJpaRepository,
			VaadinEventPublisher vaadinEventPublisher) {
		this.jobScheduler = jobScheduler;
		this.transactionTemplate = transactionTemplate;
		this.outboxJdbcRepository = outboxJdbcRepository;
		this.outboxVersionJpaRepository = outboxVersionJpaRepository;
		this.vaadinEventPublisher = vaadinEventPublisher;
	}

	@Scheduled(fixedDelay = 5000)
	public void relay() {
		var outboxProjections = outboxJdbcRepository.claimProcessing(20);
		for (var outbox : outboxProjections) {
			log.info("Relaying {}", outbox);
			// var aggregateId = outboxEntity.getAggregateId();
			// var version = outboxEntity.getVersion();
			// jobScheduler.
			// transactionTemplate.executeWithoutResult(_ -> {
			// outboxVersionJpaRepository.findByAggregateIdLocked(aggregateId)
			// .ifPresent(versionEntity -> {
			// if (Objects.equals(versionEntity.getCurrentVersion(), version)) {
			// Integer nextVersion = versionEntity.getCurrentVersion() + 1;
			// try {
			// Thread.sleep(1000);
			// } catch (InterruptedException e) {
			// throw new RuntimeException(e);
			// }
			// } else {
			// // reclaim
			// }
			// });
			// });
		}

	}

}
