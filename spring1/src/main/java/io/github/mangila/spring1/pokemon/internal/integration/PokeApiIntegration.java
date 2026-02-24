package io.github.mangila.spring1.pokemon.internal.integration;

import io.github.mangila.spring1.pokemon.internal.config.PokeApiConfig;
import io.github.mangila.spring1.pokemon.internal.integration.api.GamesApi;
import io.github.mangila.spring1.pokemon.internal.integration.api.PokemonApi;
import io.github.mangila.spring1.pokemon.internal.integration.client.ApiClient;
import java.net.URI;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PokeApiIntegration {

  private final GamesApi gamesApi;
  private final PokemonApi pokemonApi;
  private final RestClient spritesClient;
  private final URI spritesUrl;

  public PokeApiIntegration(PokeApiConfig pokeApiConfig, RestClient.Builder builder) {
    final ApiClient apiClient =
        new ApiClient(
            ApiClient.createDefaultHttpClientBuilder(),
            ApiClient.createDefaultObjectMapper(),
            pokeApiConfig.getUrl().toString());
    this.gamesApi = new GamesApi(apiClient);
    this.pokemonApi = new PokemonApi(apiClient);
    this.spritesUrl = pokeApiConfig.getSpritesUrl();
    this.spritesClient =
        builder
            .requestFactory(
                ClientHttpRequestFactoryBuilder.detect().build(HttpClientSettings.defaults()))
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.IMAGE_PNG_VALUE)
            .build();
  }

  public GamesApi gamesApi() {
    return gamesApi;
  }

  public byte[] getPokemonSprite(String url) {
    if (url == null || url.isBlank() || !url.startsWith(spritesUrl.toString())) {
      throw new IllegalArgumentException("Not a valid PokeAPI Sprites URL");
    }
    return spritesClient.get().uri(url).retrieve().requiredBody(byte[].class);
  }

  public PokemonApi pokemonApi() {
    return pokemonApi;
  }
}
