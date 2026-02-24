package io.github.mangila.spring3.sse;

import io.github.mangila.spring3.sse.model.SseSession;
import io.github.mangila.spring3.sse.model.SseSubscription;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class SseService {

  private static final Logger log = LoggerFactory.getLogger(SseService.class);

  private final SseRegistry registry;
  private final TaskExecutor taskExecutor;

  public SseService(
      SseRegistry registry, @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
    this.registry = registry;
    this.taskExecutor = taskExecutor;
  }

  public void broadcastHeartbeat() {
    registry.values().stream()
        .filter(Objects::nonNull)
        .forEach(
            sseSession -> {
              try {
                log.info(
                    "Sending heartbeat to channel: {} - session: {}",
                    sseSession.subscription().channel(),
                    sseSession.subscription().sessionId());
                sseSession.heartbeat();
              } catch (IOException e) {
                log.error(
                    "Failed to send heartbeat to channel: {} session: {}",
                    sseSession.subscription().channel(),
                    sseSession.subscription().sessionId(),
                    e);
              }
            });
  }

  public List<SseSession> getChannel(String channel) {
    return registry.sessions(channel);
  }

  public SseSession subscribe(SseSubscription sseSubscription) {
    final SseSession sseSession = SseSession.from(sseSubscription);
    final String channel = sseSession.subscription().channel();
    final String sessionId = sseSession.subscription().sessionId();
    log.info("Subscribing client to channel: {} - session: {}", channel, sessionId);
    registry.register(channel, sseSession);
    sseSession.onCompletion(
        () -> {
          log.info("Completed client from channel: {} - session: {}", channel, sessionId);
          unsubscribe(channel, sseSession);
        });
    sseSession.onTimeout(
        () -> {
          log.info("Timeout client from channel: {} - session: {}", channel, sessionId);
          unsubscribe(channel, sseSession);
        });
    sseSession.onError(
        throwable -> {
          log.error("Error client from channel: {} - session: {}", channel, sessionId, throwable);
          unsubscribe(channel, sseSession);
        });
    log.info("Starting consumer thread for SSE session: {}", sessionId);
    taskExecutor.execute(sseSession);
    return sseSession;
  }

  private void unsubscribe(String clientId, SseSession sseSession) {
    sseSession.stopConsumerThread();
    registry.unregister(clientId, sseSession);
  }
}
