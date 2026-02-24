package io.github.mangila.spring1;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableWireMock({
  @ConfigureWireMock(name = "pokemonApi", baseUrlProperties = "app.integration.pokemon-api.url")
})
public class AbstractIntegrationTest {

  @LocalServerPort private int port;

  private RestTestClient http;

  @InjectWireMock("pokemonApi")
  private WireMockServer mockPokemonApi;

  public RestTestClient http() {
    return http;
  }

  public WireMockServer mockPokemonApi() {
    return mockPokemonApi;
  }

  @BeforeEach
  void setUp() {
    this.http =
        RestTestClient.bindToServer().baseUrl("http://localhost:%s".formatted(port)).build();
  }
}
