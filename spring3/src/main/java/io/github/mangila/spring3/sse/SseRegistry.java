package io.github.mangila.spring3.sse;

import io.github.mangila.spring3.sse.model.SseSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SseRegistry {

  private static final Logger log = LoggerFactory.getLogger(SseRegistry.class);

  private final Map<String, List<SseSession>> registry;

  public SseRegistry() {
    this.registry = new ConcurrentHashMap<>();
  }

  public Optional<SseSession> getSession(String clientId, String sessionId) {
    final List<SseSession> sseSessions = registry.get(clientId);
    if (sseSessions == null) {
      return Optional.empty();
    }
    return registry.get(clientId).stream()
        .filter(sseSession -> sessionId.equals(sseSession.subscription().sessionId()))
        .findFirst();
  }

  public void register(String channel, SseSession sseSession) {
    registry.computeIfAbsent(channel, _ -> new CopyOnWriteArrayList<>()).add(sseSession);
  }

  public List<SseSession> sessions(String channel) {
    return Collections.unmodifiableList(registry.getOrDefault(channel, new ArrayList<>()));
  }

  public void unregister(String channel, SseSession sseSession) {
    registry.computeIfPresent(
        channel,
        (_, sessions) -> {
          final boolean removed = sessions.remove(sseSession);
          if (removed) {
            log.info(
                "Unregistered SseSession from channel {} - session: {}",
                channel,
                sseSession.subscription().sessionId());
          }
          if (sessions.isEmpty()) {
            log.info("Removed channel {} from registry", channel);
            return null;
          }
          return sessions;
        });
  }

  public List<SseSession> values() {
    return registry.values().stream().flatMap(List::stream).toList();
  }
}
