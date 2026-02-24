package io.github.mangila.spring1.pokemon.internal.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.github.tomakehurst.wiremock.common.Json;
import io.github.mangila.spring1.AbstractIntegrationTest;
import io.github.mangila.spring1.pokemon.PokemonDto;
import io.github.mangila.spring1.pokemon.internal.common.Generation;
import io.github.mangila.spring1.pokemon.internal.config.PokemonApiConfig;
import io.github.mangila.spring1.pokemon.internal.data.PokemonEntity;
import io.github.mangila.spring1.pokemon.internal.data.PokemonRepository;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

class PokemonControllerIT extends AbstractIntegrationTest {

  @Autowired private PokemonRepository repository;

  @Autowired private PokemonApiConfig pokemonApiConfig;

  private final int pokedexId = 1;
  private final String name = "bulbasaur";

  @AfterEach
  void afterEach() {
    repository.deleteAll();
  }

  @BeforeEach
  void beforeEach() {
    PokemonEntity entity = new PokemonEntity();
    entity.setId(UUID.randomUUID());
    entity.setPokedexId(pokedexId);
    entity.setName(name);
    repository.save(entity);
  }

  @Test
  void deletePokemon() {
    http()
        .delete()
        .uri("api/v1/pokemon/{pokedexId}", pokedexId)
        .exchange()
        .expectStatus()
        .isNoContent();
    http()
        .get()
        .uri("api/v1/pokemon/{pokedexId}", pokedexId)
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void findAllPokemon() {
    http()
        .get()
        .uri("api/v1/pokemon")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(new ParameterizedTypeReference<List<PokemonDto>>() {})
        .consumeWith(
            response -> {
              List<PokemonDto> list = response.getResponseBody();
              assertThat(list).isNotNull().isNotEmpty();
              assertThat(list).anyMatch(p -> p.pokedexId() == pokedexId && p.name().equals(name));
            });
  }

  @Test
  void findPokemonByPokedexId() {
    http()
        .get()
        .uri("api/v1/pokemon/{pokedexId}", 1)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(PokemonDto.class)
        .consumeWith(
            response -> {
              PokemonDto body = response.getResponseBody();
              assertThat(body).isNotNull();
              assertThat(body.pokedexId()).isEqualTo(pokedexId);
              assertThat(body.name()).isEqualTo(name);
            });
  }

  @Test
  void findPokemonByPokedexIdBadRequest() {
    http()
        .get()
        .uri("api/v1/pokemon/{id}", -1)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody(ProblemDetail.class)
        .consumeWith(
            response -> {
              final ProblemDetail problemDetail = response.getResponseBody();
              assertThat(problemDetail.getDetail()).isEqualTo("Validation failed");
              assertThat(problemDetail.getProperties().containsKey("errors")).isTrue();
            });
    http()
        .get()
        .uri("api/v1/pokemon/{id}", 152)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody(ProblemDetail.class)
        .consumeWith(
            response -> {
              final ProblemDetail problemDetail = response.getResponseBody();
              assertThat(problemDetail.getDetail()).isEqualTo("Validation failed");
              assertThat(problemDetail.getProperties().containsKey("errors")).isTrue();
            });
  }

  @Test
  void findPokemonByPokedexIdNotFound() {
    http().get().uri("api/v1/pokemon/{pokedexId}", 99).exchange().expectStatus().isNotFound();
  }

  @Test
  void schedulePokemon() {
    repository.deleteAll();
    @Language("JSON")
    String payload =
        """
          {
          "pokemon_species": [
            {
              "name": "%s",
              "url": "%s/%s"
            }
          ]
        }
        """
            .formatted(name, pokemonApiConfig.getUrl(), name);
    mockPokemonApi()
        .stubFor(
            get("/generation/generation-i")
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withJsonBody(Json.node(payload))
                        .withStatus(200)));
    payload =
        """
        {
        "id": %s,
        "name": "%s"
        }
        """
            .formatted(pokedexId, name);
    mockPokemonApi()
        .stubFor(
            get("/bulbasaur")
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withJsonBody(Json.node(payload))
                        .withStatus(200)));

    http()
        .post()
        .uri("api/v1/pokemon/schedule/{generation}", Generation.GENERATION_I)
        .exchange()
        .expectStatus()
        .isOk();
    await()
        .atMost(Duration.ofSeconds(25))
        .untilAsserted(
            () -> {
              mockPokemonApi().verify(1, getRequestedFor(urlEqualTo("/bulbasaur")));
            });
  }
}
