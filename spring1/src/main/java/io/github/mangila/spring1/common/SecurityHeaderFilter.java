package io.github.mangila.spring1.common;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Security headers for the same-origin REST API and Angular SPA.
 *
 * @see <a href= "https://owasp.github.io/www-project-secure-headers/response-headers/">OWASP Secure
 *     Headers</a>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeaderFilter implements Filter {

  // Angular injects component styles and the production build can inline critical
  // CSS.
  // Scripts remain restricted to same-origin files; inline scripts and eval are
  // disallowed.
  private static final String CONTENT_SECURITY_POLICY =
      "default-src 'self'; "
          + "base-uri 'self'; "
          + "object-src 'none'; "
          + "frame-ancestors 'none'; "
          + "frame-src 'none'; "
          + "form-action 'self'; "
          + "script-src 'self'; "
          + "script-src-attr 'none'; "
          + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
          + "font-src 'self' https://fonts.gstatic.com; "
          + "img-src 'self' data:; "
          + "connect-src 'self'; "
          + "media-src 'none'; "
          + "worker-src 'self'; "
          + "manifest-src 'self';";

  private static final String PERMISSIONS_POLICY =
      "accelerometer=(), autoplay=(), camera=(), display-capture=(), "
          + "encrypted-media=(), fullscreen=(), geolocation=(), gyroscope=(), "
          + "magnetometer=(), microphone=(), payment=(), picture-in-picture=(), "
          + "publickey-credentials-get=(), usb=()";

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletResponse httpResponse = (HttpServletResponse) response;
    httpResponse.setHeader("X-Frame-Options", "DENY");
    httpResponse.setHeader("X-Content-Type-Options", "nosniff");
    // The container must recognize HTTPS, including trusted proxy forwarding when
    // applicable.
    // Subdomain coverage and preload require a separate domain-wide deployment
    // decision.
    if (request.isSecure()) {
      httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000");
    }
    httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
    httpResponse.setHeader("Content-Security-Policy", CONTENT_SECURITY_POLICY);
    httpResponse.setHeader("Permissions-Policy", PERMISSIONS_POLICY);
    httpResponse.setHeader("Cross-Origin-Opener-Policy", "same-origin");
    httpResponse.setHeader("Cross-Origin-Resource-Policy", "same-origin");
    httpResponse.setHeader("Origin-Agent-Cluster", "?1");
    httpResponse.setHeader("X-Permitted-Cross-Domain-Policies", "none");
    // Disable legacy browser XSS filters; CSP supplies the script restrictions.
    httpResponse.setHeader("X-XSS-Protection", "0");

    // COEP requires every embedded third-party resource to opt in (including Google
    // Fonts
    // stylesheets). This application does not need SharedArrayBuffer/cross-origin
    // isolation.
    // Cache-Control belongs to each endpoint/resource; Clear-Site-Data belongs to
    // logout.

    chain.doFilter(request, response);
  }
}
