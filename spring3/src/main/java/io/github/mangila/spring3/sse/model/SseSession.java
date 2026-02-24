package io.github.mangila.spring3.sse.model;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public record SseSession(
    SseSubscription subscription,
    BlockingQueue<Set<ResponseBodyEmitter.DataWithMediaType>> buffer,
    SseEmitter emitter)
    implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(SseSession.class);

  public static final Set<ResponseBodyEmitter.DataWithMediaType> EOF_SENTINEL = Set.of();
  public static final Set<ResponseBodyEmitter.DataWithMediaType> HEARTBEAT =
      SseEmitter.event().name("heartbeat").data("").build();

  public static SseSession from(SseSubscription subscription) {
    final SseEmitter sseEmitter = new SseEmitter(Long.MAX_VALUE);
    final BlockingQueue<Set<ResponseBodyEmitter.DataWithMediaType>> buffer =
        new ArrayBlockingQueue<>(256, true);
    return new SseSession(subscription, buffer, sseEmitter);
  }

  private void send(Set<ResponseBodyEmitter.DataWithMediaType> event) throws IOException {
    try {
      emitter.send(event);
    } catch (IllegalStateException e) {
      log.error("Stream closed", e);
    }
  }

  public boolean tryPush(Set<ResponseBodyEmitter.DataWithMediaType> event, Duration timeout) {
    try {
      return buffer.offer(event, timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      log.error("Interrupted while pushing event to buffer", e);
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted while pushing event to buffer", e);
    }
  }

  public void onCompletion(Runnable callback) {
    emitter.onCompletion(callback);
  }

  public void onTimeout(Runnable callback) {
    emitter.onTimeout(callback);
  }

  public void onError(Consumer<Throwable> callback) {
    emitter.onError(callback);
  }

  public void heartbeat() throws IOException {
    send(HEARTBEAT);
  }

  public void stopConsumerThread() {
    buffer.clear();
    tryPush(EOF_SENTINEL, Duration.ofMinutes(1));
  }

  @Override
  public void run() {
    while (!Thread.currentThread().isInterrupted()) {
      try {
        final Set<ResponseBodyEmitter.DataWithMediaType> event = buffer.take();
        if (event == EOF_SENTINEL) {
          log.info("EOF_SENTINEL received, stopping SSE consumer thread");
          break;
        }
        send(event);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }
}
