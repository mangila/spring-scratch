package io.github.mangila.spring3.web;

import jakarta.servlet.http.Cookie;
import org.springframework.stereotype.Component;

@Component
public class CookieProvider {

  public static final String COOKIE_CLIENT_ID = "clientId";

  public Cookie createClientIdCookie(String value) {
    final Cookie cookie = new Cookie(COOKIE_CLIENT_ID, value);
    cookie.setPath("/");
    cookie.setMaxAge(86400);
    cookie.setHttpOnly(true);
    cookie.setSecure(false);
    return cookie;
  }
}
