package io.github.mangila.spring3.fibonacci;

import io.github.mangila.spring3.web.CookieProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/fibonacci")
public class FibonacciController {

  private final TaskExecutor taskExecutor;
  private final FibonacciService fibonacciService;

  public FibonacciController(
      @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor,
      FibonacciService fibonacciService) {
    this.taskExecutor = taskExecutor;
    this.fibonacciService = fibonacciService;
  }

  @PostMapping
  public ResponseEntity<Void> stream(
      @CookieValue(CookieProvider.COOKIE_CLIENT_ID) String clientId,
      @RequestHeader("X-Session-Id") String sessionId,
      @ModelAttribute FibonacciStreamCommand command) {
    taskExecutor.execute(() -> fibonacciService.stream(clientId, sessionId, command));
    return ResponseEntity.ok().build();
  }
}
