package com.github.mangila.app.director.scheduler;

import com.github.mangila.app.director.outbox.DirectorOutboxScheduler;
import com.github.mangila.app.director.properties.DirectorProperties;
import com.github.mangila.app.director.scheduler.task.DirectorOutboxMonitorTask;
import com.github.mangila.app.director.scheduler.task.DirectorOutboxPurgeTask;
import com.github.mangila.app.director.scheduler.task.DirectorOutboxRecoverTask;
import com.github.mangila.app.director.scheduler.task.DirectorOutboxRelayTask;
import jakarta.annotation.PostConstruct;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

@Service
public class DirectorScheduler {

    private static final Logger log = LoggerFactory.getLogger(DirectorScheduler.class);

    private final SimpleAsyncTaskScheduler taskScheduler;
    private final StorageProvider storageProvider;
    private final DirectorOutboxScheduler directorOutboxScheduler;
    private final LockingTaskExecutor lockingTaskExecutor;
    private final DirectorProperties directorProperties;

    public DirectorScheduler(SimpleAsyncTaskScheduler taskScheduler,
                          StorageProvider storageProvider,
                          DirectorProperties directorProperties,
                          DirectorOutboxScheduler directorOutboxScheduler,
                          LockingTaskExecutor lockingTaskExecutor) {
        this.taskScheduler = taskScheduler;
        this.storageProvider = storageProvider;
        this.directorProperties = directorProperties;
        this.directorOutboxScheduler = directorOutboxScheduler;
        this.lockingTaskExecutor = lockingTaskExecutor;
    }

    @PostConstruct
    public void init() {
        var outbox = directorProperties.getOutbox();
        if (outbox.isEnabled()) {
            log.info("Director Outbox is enabled");
            if (outbox.getRelay().isEnabled()) {
                log.info("Director Outbox relay is enabled");
                final var props = directorProperties.getOutbox().getRelay();
                final var task = new DirectorOutboxRelayTask(props, directorOutboxScheduler);
                taskScheduler.schedule(task, new CronTrigger(props.getCron()));
            }
            if (outbox.getMonitor().isEnabled()) {
                log.info("Director Outbox monitor is enabled");
                final var props = directorProperties.getOutbox().getMonitor();
                final var task = new DirectorOutboxMonitorTask(props, lockingTaskExecutor, directorOutboxScheduler);
                taskScheduler.schedule(task, new CronTrigger(props.getCron()));
            }
            if (outbox.getRecover().isEnabled()) {
                log.info("Director Outbox recover is enabled");
                final var props = directorProperties.getOutbox().getRecover();
                final var task = new DirectorOutboxRecoverTask(props, lockingTaskExecutor, storageProvider, directorOutboxScheduler);
                taskScheduler.schedule(task, new CronTrigger(props.getCron()));
            }
            if (outbox.getPurge().isEnabled()) {
                log.info("Director Outbox purge is enabled");
                final var props = directorProperties.getOutbox().getPurge();
                final var task = new DirectorOutboxPurgeTask(props, directorOutboxScheduler);
                taskScheduler.schedule(task, new CronTrigger(props.getCron()));
            }
        }
    }

}
