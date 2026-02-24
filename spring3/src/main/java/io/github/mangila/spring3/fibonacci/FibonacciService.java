package io.github.mangila.spring3.fibonacci;

import io.github.mangila.spring3.sse.SseRegistry;
import io.github.mangila.spring3.sse.model.SseSession;
import io.github.mangila.spring3.web.FragmentProvider;
import java.math.BigInteger;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class FibonacciService {

  private static final Logger log = LoggerFactory.getLogger(FibonacciService.class);
  private static final Duration TIMEOUT = Duration.ofSeconds(10);

  private final SseRegistry sseRegistry;
  private final FragmentProvider fragmentProvider;

  public FibonacciService(SseRegistry sseRegistry, FragmentProvider fragmentProvider) {
    this.sseRegistry = sseRegistry;
    this.fragmentProvider = fragmentProvider;
  }

  public void stream(SseSession sseSession, String lastEventId) {
    BigInteger previous2 = BigInteger.ZERO; // n - 2
    sseSession.tryPush(createEvent(1, previous2), TIMEOUT);
    BigInteger previous1 = BigInteger.ONE; // n - 1
    sseSession.tryPush(createEvent(2, previous1), TIMEOUT);
    BigInteger current;
  }

  public void stream(String clientId, String sessionId, FibonacciStreamCommand command) {
    final SseSession sseSession =
        sseRegistry
            .getSession(clientId, sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found"));
    log.info("Streaming fibonacci sequence for client {} and session {}", clientId, sessionId);
    BigInteger previous2 = BigInteger.ZERO; // n - 2
    sseSession.tryPush(createEvent(1, previous2), TIMEOUT);
    BigInteger previous1 = BigInteger.ONE; // n - 1
    sseSession.tryPush(createEvent(2, previous1), TIMEOUT);
    BigInteger current;

    for (int i = 2; i < command.limit(); i++) {
      current = previous1.add(previous2);
      sseSession.tryPush(createEvent(i + 1, current), TIMEOUT);
      previous2 = previous1;
      previous1 = current;
      try {
        TimeUnit.MILLISECONDS.sleep(command.delayMillis());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
    }
  }

  private Set<ResponseBodyEmitter.DataWithMediaType> createEvent(int sequence, BigInteger value) {
    return SseEmitter.event()
        .id(String.valueOf(sequence))
        .name("fibonacci")
        .data("<div> hello </div>", MediaType.TEXT_HTML)
        .reconnectTime(2000L)
        .build();
  }
}
