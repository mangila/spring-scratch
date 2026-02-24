package io.github.mangila.spring1;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

@Import(TestcontainersConfiguration.class)
@EnableWireMock({
  @ConfigureWireMock(name = "pokemonApi", baseUrlProperties = "app.integration.pokemon-api.url")
})
public class AbstractIntegrationTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractIntegrationTest.class);

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

  public void setUpPokemonApi() {
    @Language("JSON")
    String payload =
        """
          {
                  "pokemon_species": [
                    {
                      "name": "missingno",
                      "url": "%s/missingno"
                    }
                  ]
                }
        """
            .formatted(mockPokemonApi.baseUrl());
    mockPokemonApi.stubFor(
        get("/generation/generation-i")
            .willReturn(
                aResponse()
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(payload)
                    .withStatus(200)));
    payload =
        """
        {
        "id": 0,
        "name": "missingno",
        "generation": {
          "name": "generation-i",
          "url": ""
          }
        }
        """;
    mockPokemonApi.stubFor(
        get("/missingno")
            .willReturn(
                aResponse()
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(payload)
                    .withStatus(200)));
  }

  @BeforeEach
  void setUp() {
    this.http =
        RestTestClient.bindToServer()
            .baseUrl("http://localhost:%s".formatted(port))
            .requestInterceptor(
                (request, body, execution) -> {
                  LOGGER.info(
                      """

                      <== TEST REQUEST ==>
                      Method: {}
                      URI: {}
                      Headers: {}
                      """,
                      request.getMethod(),
                      request.getURI(),
                      request.getHeaders());
                  final ClientHttpResponse response = execution.execute(request, body);
                  LOGGER.info(
                      """

                      <== TEST RESPONSE ==>
                      Status: {}
                      Headers: {}
                      """,
                      response.getStatusCode(),
                      response.getHeaders());
                  return response;
                })
            .build();
  }

  @AfterEach
  void tearDown() {
    mockPokemonApi.resetAll();
  }
}
