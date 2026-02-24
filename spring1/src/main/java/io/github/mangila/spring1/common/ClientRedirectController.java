package io.github.mangila.spring1.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ClientRedirectController {

  @GetMapping(
      value = "{path:^(?!api|actuator|swagger-ui|h2-console|browser|favicon\\.ico)[^\\.]*}/**")
  public String redirectClient() {
    return "forward:/browser/index.html";
  }
}
