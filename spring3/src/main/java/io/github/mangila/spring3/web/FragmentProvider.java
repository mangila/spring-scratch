package io.github.mangila.spring3.web;

import org.springframework.stereotype.Component;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Component
public class FragmentProvider {

  private final SpringTemplateEngine templateEngine;

  public FragmentProvider(SpringTemplateEngine templateEngine) {
    this.templateEngine = templateEngine;
  }
}
