package io.github.mangila.spring3.sse;

import java.util.concurrent.TimeUnit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SseScheduler {

  private final SseService sseService;

  public SseScheduler(SseService sseService) {
    this.sseService = sseService;
  }

  @Scheduled(fixedRate = 5, timeUnit = TimeUnit.SECONDS)
  public void broadcastHeartbeat() {
    sseService.broadcastHeartbeat();
  }
}
