package io.github.mangila.spring3.web;

import io.github.mangila.spring3.sse.model.SseSubscription;
import java.util.List;
import org.intellij.lang.annotations.Language;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Component
public class FragmentProvider {

  private final SpringTemplateEngine templateEngine;

  public FragmentProvider(SpringTemplateEngine templateEngine) {
    this.templateEngine = templateEngine;
  }

  public @Language("HTML") String getFibonacciCardFragment(int sequence, String fibonacciNumber) {
    Context context = new Context();
    context.setVariable("sequence", sequence);
    context.setVariable("fibonacciNumber", fibonacciNumber);
    return templateEngine.process("fragments/htmx/fibonacci-card", context);
  }

  public @Language("HTML") String getSessionCardFragment(List<SseSubscription> sseSubscriptions) {
    Context context = new Context();
    context.setVariable("sseSubscriptions", sseSubscriptions);
    return templateEngine.process("fragments/htmx/session-card", context);
  }
}
