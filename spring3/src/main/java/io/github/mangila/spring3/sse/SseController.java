package io.github.mangila.spring3.sse;

import static io.github.mangila.spring3.web.CookieProvider.COOKIE_CLIENT_ID;

import io.github.mangila.spring3.sse.model.SseSession;
import io.github.mangila.spring3.sse.model.SseSubscription;
import io.github.mangila.spring3.web.FragmentProvider;
import java.util.List;
import org.intellij.lang.annotations.Language;
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

  public SseController(FragmentProvider fragmentProvider, SseService sseService) {
    this.fragmentProvider = fragmentProvider;
    this.sseService = sseService;
  }

  @GetMapping(value = "metadata", produces = MediaType.TEXT_HTML_VALUE)
  public String metadata(@CookieValue(name = COOKIE_CLIENT_ID) String clientId) {
    final List<SseSession> sseSessions = sseService.getChannel(clientId);
    final List<SseSubscription> sseSubscriptions =
        sseSessions.stream().map(SseSession::subscription).toList();
    @Language("HTML")
    final String html = fragmentProvider.getSessionCardFragment(sseSubscriptions);
    return html;
  }

  @GetMapping(value = "subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter subscribe(
      @CookieValue(name = COOKIE_CLIENT_ID) String clientId,
      @RequestHeader(name = "User-Agent") String userAgent,
      @RequestHeader(name = "Last-Event-ID", required = false) String lastEventId,
      @RequestParam("sessionId") String sessionId) {
    log.info("Last Event ID received: {} -- do nothing...", lastEventId);
    final SseSubscription subscription = new SseSubscription(clientId, userAgent, sessionId);
    final SseSession sseSession = sseService.subscribe(subscription);
    return sseSession.emitter();
  }
}
