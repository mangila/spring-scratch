package io.github.mangila.spring3.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TemplateController {

  private final BuildProperties buildProperties;
  private final CookieProvider cookieProvider;

  public TemplateController(BuildProperties buildProperties, CookieProvider cookieProvider) {
    this.buildProperties = buildProperties;
    this.cookieProvider = cookieProvider;
  }

  @GetMapping("/")
  public String index(
      @CookieValue(name = CookieProvider.COOKIE_CLIENT_ID, required = false) String cookieValue,
      HttpServletResponse response,
      Model model) {

    if (cookieValue == null || cookieValue.isBlank()) {
      cookieValue = UUID.randomUUID().toString();
      final Cookie cookie = cookieProvider.createClientIdCookie(cookieValue);
      response.addCookie(cookie);
    }

    model.addAttribute("title", "Spring Boot SSE & Thymeleaf Fibonacci");
    model.addAttribute("name", buildProperties.getName());
    model.addAttribute("group", buildProperties.getGroup());
    model.addAttribute("version", buildProperties.getVersion());
    return "index";
  }
}
