package io.github.mangila.spring3.sse;

import static io.github.mangila.spring3.web.CookieProvider.COOKIE_CLIENT_ID;

import io.github.mangila.spring3.fibonacci.FibonacciService;
import io.github.mangila.spring3.sse.model.SseSession;
import io.github.mangila.spring3.sse.model.SseSubscription;
import io.github.mangila.spring3.web.FragmentProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/sse")
public class SseController {

  private static final Logger log = LoggerFactory.getLogger(SseController.class);

  private final FragmentProvider fragmentProvider;
  private final SseService sseService;
  private final FibonacciService fibonacciService;

  public SseController(
      FragmentProvider fragmentProvider, SseService sseService, FibonacciService fibonacciService) {
    this.fragmentProvider = fragmentProvider;
    this.sseService = sseService;
    this.fibonacciService = fibonacciService;
  }

  @GetMapping(value = "metadata", produces = MediaType.TEXT_HTML_VALUE)
  public String metadata(@CookieValue(name = COOKIE_CLIENT_ID) String clientId) {
    return "<div> hello </div>";
  }

  @GetMapping(value = "stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter stream(
      @CookieValue(name = COOKIE_CLIENT_ID) String clientId,
      @RequestHeader(name = "User-Agent") String userAgent,
      @RequestHeader(name = "Last-Event-ID", required = false) String lastEventId,
      @RequestHeader(name = "X-Session-Id") String sessionId,
      @RequestParam(name = "limit") int limit,
      @RequestParam(name = "delayMillis") int delayMillis) {
    log.info("Last Event ID received: {}", lastEventId);
    final SseSubscription subscription = new SseSubscription(clientId, userAgent, sessionId);
    final SseSession sseSession = sseService.subscribe(subscription);
    fibonacciService.stream(sseSession, lastEventId);
    return sseSession.emitter();
  }
}
