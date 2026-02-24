package io.github.mangila.spring1;

import org.junit.jupiter.api.Test;

class SmokeIT extends AbstractIntegrationTest {

  @Test
  void smoke() {
    http()
        .get()
        .uri("/actuator/health")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo("UP");
  }
}
