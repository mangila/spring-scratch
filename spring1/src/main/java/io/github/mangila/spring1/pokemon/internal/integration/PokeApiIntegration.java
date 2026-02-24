package io.github.mangila.spring1.pokemon.internal.integration;

import io.github.mangila.spring1.pokemon.internal.config.PokeApiConfig;
import io.github.mangila.spring1.pokemon.internal.integration.api.GamesApi;
import io.github.mangila.spring1.pokemon.internal.integration.api.PokemonApi;
import io.github.mangila.spring1.pokemon.internal.integration.client.ApiClient;
import org.springframework.stereotype.Service;

@Service
public class PokeApiIntegration {

  private final GamesApi gamesApi;
  private final PokemonApi pokemonApi;

  public PokeApiIntegration(PokeApiConfig pokeApiConfig) {
    final ApiClient apiClient =
        new ApiClient(
            ApiClient.createDefaultHttpClientBuilder(),
            ApiClient.createDefaultObjectMapper(),
            pokeApiConfig.getUrl().toString());
    this.gamesApi = new GamesApi(apiClient);
    this.pokemonApi = new PokemonApi(apiClient);
  }

  public GamesApi gamesApi() {
    return gamesApi;
  }

  public PokemonApi pokemonApi() {
    return pokemonApi;
  }
}
