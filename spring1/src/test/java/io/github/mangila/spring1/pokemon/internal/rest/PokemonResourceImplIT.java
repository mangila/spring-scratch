package io.github.mangila.spring1.pokemon.internal.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.github.mangila.spring1.AbstractIntegrationTest;
import io.github.mangila.spring1.pokemon.PokemonCreatedEvent;
import io.github.mangila.spring1.pokemon.internal.data.PokemonEntity;
import io.github.mangila.spring1.pokemon.internal.data.PokemonRepository;
import io.github.mangila.spring1.pokemon.internal.integration.S3Integration;
import io.github.mangila.spring1.pokemon.internal.rest.model.Generation;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.PublishedEvents;
import software.amazon.awssdk.core.sync.RequestBody;
import tools.jackson.databind.node.ObjectNode;

@ApplicationModuleTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    extraIncludes = {"config", "common"})
class PokemonResourceImplIT extends AbstractIntegrationTest {

  @Autowired private PokemonRepository repository;
  @Autowired private S3Integration s3Integration;

  private final int pokedexId = 0;
  private final String name = "missingno";

  @AfterEach
  void afterEach() {
    repository.deleteAll();
  }

  @Test
  void deletePokemon() {
    seedPokemon();

    http()
        .delete()
        .uri("api/v1/pokemon/id/{id}", pokedexId)
        .exchange()
        .expectStatus()
        .isNoContent();
    http().delete().uri("api/v1/pokemon/id/{id}", pokedexId).exchange().expectStatus().isNotFound();
  }

  @Test
  void findAllPokemon() {
    seedPokemon();

    http()
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("api/v1/pokemon")
                    .queryParam("page", 0)
                    .queryParam("size", 10)
                    .build())
        .exchangeSuccessfully()
        .expectBody(ObjectNode.class)
        .consumeWith(
            response -> {
              ObjectNode json = response.getResponseBody();
              assertThatJson(json).isNotNull().isObject().containsOnlyKeys("page", "content");
              assertThatJson(json.get("content")).isArray().hasSize(1);
              assertThatJson(json.get("page"))
                  .isObject()
                  .containsOnlyKeys("totalElements", "totalPages", "size", "number");
            });
  }

  @Test
  void findAllPokemonBadRequest() {
    http()
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("api/v1/pokemon")
                    .queryParam("page", Integer.MIN_VALUE)
                    .queryParam("size", 10)
                    .build())
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody(ObjectNode.class)
        .consumeWith(
            response -> {
              ObjectNode json = response.getResponseBody();
              assertThatJson(json)
                  .isNotNull()
                  .isObject()
                  .containsOnlyKeys("detail", "instance", "status", "title", "errors")
                  .containsEntry("detail", "Validation failed")
                  .node("errors")
                  .isArray()
                  .hasSize(1)
                  .element(0)
                  .isString()
                  .isNotBlank()
                  .asString()
                  .contains("must be greater than or equal to 0");
            });

    http()
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("api/v1/pokemon")
                    .queryParam("page", 1)
                    .queryParam("size", 101)
                    .build())
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody(ObjectNode.class)
        .consumeWith(
            response -> {
              ObjectNode json = response.getResponseBody();
              assertThatJson(json)
                  .isNotNull()
                  .isObject()
                  .containsOnlyKeys("detail", "instance", "status", "title", "errors")
                  .containsEntry("detail", "Validation failed")
                  .node("errors")
                  .isArray()
                  .hasSize(1)
                  .element(0)
                  .isString()
                  .isNotBlank()
                  .asString()
                  .contains("must be less than or equal to 100");
            });
  }

  @Test
  void findAllPokemonEmpty() {
    http()
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("api/v1/pokemon")
                    .queryParam("page", Integer.MAX_VALUE)
                    .queryParam("size", 10)
                    .build())
        .exchangeSuccessfully()
        .expectBody(ObjectNode.class)
        .consumeWith(
            response -> {
              ObjectNode json = response.getResponseBody();
              assertThatJson(json).isNotNull().isObject().containsOnlyKeys("page", "content");
              assertThatJson(json.get("content")).isArray().isEmpty();
              assertThatJson(json.get("page"))
                  .isObject()
                  .containsOnlyKeys("totalElements", "totalPages", "size", "number");
            });
  }

  @Test
  void findPokemonByName() {
    seedPokemon();

    http()
        .get()
        .uri("api/v1/pokemon/name/{name}", name)
        .exchangeSuccessfully()
        .expectBody(ObjectNode.class)
        .consumeWith(
            response -> {
              ObjectNode json = response.getResponseBody();
              assertThatJson(json)
                  .isNotNull()
                  .isObject()
                  .containsOnlyKeys("name", "pokedexId", "imageUrl", "generation")
                  .containsEntry("name", name);
            });
  }

  @Test
  void findPokemonByNameBadRequest() {
    http()
        .get()
        .uri("api/v1/pokemon/name/{name}", "a")
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody(ObjectNode.class)
        .consumeWith(
            response -> {
              ObjectNode json = response.getResponseBody();
              assertThatJson(json)
                  .isNotNull()
                  .isObject()
                  .containsOnlyKeys("detail", "instance", "status", "title", "errors")
                  .containsEntry("detail", "Validation failed")
                  .node("errors")
                  .isArray()
                  .hasSize(1)
                  .element(0)
                  .isString()
                  .isNotBlank()
                  .asString()
                  .contains("size must be between 2 and 100");
            });
  }

  @Test
  void findPokemonByNameNotFound() {
    http()
        .get()
        .uri("api/v1/pokemon/name/{name}", "not_found")
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody(ObjectNode.class)
        .consumeWith(
            response -> {
              ObjectNode json = response.getResponseBody();
              assertThatJson(json)
                  .isNotNull()
                  .isObject()
                  .containsOnlyKeys("detail", "instance", "status", "title")
                  .containsEntry("detail", "Resource not found");
            });
  }

  @Test
  void findPokemonByPokedexId() {
    seedPokemon();

    http()
        .get()
        .uri("api/v1/pokemon/id/{id}", pokedexId)
        .exchangeSuccessfully()
        .expectBody(ObjectNode.class)
        .consumeWith(
            response -> {
              ObjectNode json = response.getResponseBody();
              assertThatJson(json)
                  .isNotNull()
                  .isObject()
                  .containsOnlyKeys("name", "pokedexId", "imageUrl", "generation")
                  .containsEntry("pokedexId", pokedexId)
                  .containsEntry("generation", "generation-i");
            });
  }

  @Test
  void findPokemonByPokedexIdBadRequest() {
    http()
        .get()
        .uri("api/v1/pokemon/id/{id}", -1)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody(ObjectNode.class)
        .consumeWith(
            response -> {
              ObjectNode json = response.getResponseBody();
              assertThatJson(json)
                  .isNotNull()
                  .isObject()
                  .containsOnlyKeys("detail", "instance", "status", "title", "errors")
                  .containsEntry("detail", "Validation failed")
                  .node("errors")
                  .isArray()
                  .hasSize(1)
                  .element(0)
                  .isString()
                  .isNotBlank()
                  .asString()
                  .contains("must be greater than or equal to 0");
            });
    http()
        .get()
        .uri("api/v1/pokemon/id/{id}", 152)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody(ObjectNode.class)
        .consumeWith(
            response -> {
              ObjectNode json = response.getResponseBody();
              assertThatJson(json)
                  .isNotNull()
                  .isObject()
                  .containsOnlyKeys("detail", "instance", "status", "title", "errors")
                  .containsEntry("detail", "Validation failed")
                  .node("errors")
                  .isArray()
                  .hasSize(1)
                  .element(0)
                  .isString()
                  .isNotBlank()
                  .asString()
                  .contains("must be less than or equal to 151");
            });
  }

  @Test
  void findPokemonByPokedexIdNotFound() {
    http()
        .get()
        .uri("api/v1/pokemon/id/{id}", 99)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody(ObjectNode.class)
        .consumeWith(
            response -> {
              ObjectNode json = response.getResponseBody();
              assertThatJson(json)
                  .isNotNull()
                  .isObject()
                  .containsOnlyKeys("detail", "instance", "status", "title")
                  .containsEntry("detail", "Resource not found");
            });
  }

  @Test
  void findPokemonImageByName() {
    byte[] imageBytes = new byte[] {1, 2, 3, 4};
    s3Integration.putObject(name, RequestBody.fromBytes(imageBytes));

    http()
        .get()
        .uri("api/v1/pokemon/image/{name}", name)
        .exchangeSuccessfully()
        .expectHeader()
        .contentType(MediaType.IMAGE_PNG)
        .expectHeader()
        .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
        .expectBody(byte[].class)
        .consumeWith(
            response -> {
              byte[] body = response.getResponseBody();
              assertThat(body).isEqualTo(imageBytes);
            });
  }

  @Test
  void findPokemonImageByNameBadRequest() {
    http()
        .get()
        .uri("api/v1/pokemon/image/{name}", "a")
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody(ObjectNode.class)
        .consumeWith(
            response -> {
              ObjectNode json = response.getResponseBody();
              assertThatJson(json)
                  .isNotNull()
                  .isObject()
                  .containsOnlyKeys("detail", "instance", "status", "title", "errors")
                  .containsEntry("detail", "Validation failed")
                  .node("errors")
                  .isArray()
                  .hasSize(1)
                  .element(0)
                  .isString()
                  .isNotBlank()
                  .asString()
                  .contains("size must be between 2 and 100");
            });
  }

  @Test
  void findPokemonImageByNameNotFound() {
    http()
        .get()
        .uri("api/v1/pokemon/image/{name}", "not_found")
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody(ObjectNode.class)
        .consumeWith(
            response -> {
              ObjectNode json = response.getResponseBody();
              assertThatJson(json)
                  .isNotNull()
                  .isObject()
                  .containsOnlyKeys("detail", "instance", "status", "title")
                  .containsEntry("detail", "Resource not found");
            });
  }

  @Test
  void schedulePokemon(PublishedEvents events) {
    super.setUpPokemonApi();
    http()
        .post()
        .uri("api/v1/pokemon/schedule/{generation}", Generation.GENERATION_I)
        .exchange()
        .expectStatus()
        .isOk();
    await()
        .atMost(Duration.ofSeconds(30))
        .untilAsserted(
            () -> {
              mockPokemonApi()
                  .verify(1, getRequestedFor(urlEqualTo("/api/v2/pokemon-species/missingno/")));
              mockPokemonApi().verify(1, getRequestedFor(urlEqualTo("/api/v2/pokemon/missingno/")));
              mockPokemonApi().verify(1, getRequestedFor(urlEqualTo("/imageUrl/missingno")));
              assertThat(repository.count()).isEqualTo(1);
              assertThat(events.eventOfTypeWasPublished(PokemonCreatedEvent.class)).isTrue();
              assertThat(s3Integration.getObject(name)).isEqualTo(new byte[] {1, 2, 3, 4});
            });
  }

  void seedPokemon() {
    PokemonEntity entity = new PokemonEntity();
    entity.setId(UUID.randomUUID());
    entity.setPokedexId(pokedexId);
    entity.setName(name);
    String host = super.mockPokemonApi().baseUrl();
    entity.setImageUrl("%s/api/v1/image/%s".formatted(host, name));
    entity.setGeneration(Generation.GENERATION_I.getValue());
    repository.save(entity);
  }
}
