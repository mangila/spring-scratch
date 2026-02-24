package io.github.mangila.spring1.pokemon.internal.integration;

import io.github.mangila.spring1.pokemon.internal.common.Generation;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PokemonIntegration {

  private final RestClient pokemonApiRestClient;

  public PokemonIntegration(@Qualifier("pokemonApiClient") RestClient client) {
    this.pokemonApiRestClient = client;
  }

  public PokemonResponse fetchPokemonByUrl(String url) {
    return pokemonApiRestClient
        .mutate()
        .baseUrl(url)
        .build()
        .get()
        .exchange(
            (_, clientResponse) -> {
              if (clientResponse.getStatusCode().is2xxSuccessful()) {
                return clientResponse.bodyTo(PokemonResponse.class);
              } else {
                return PokemonResponse.MISSING_NO;
              }
            });
  }

  public GenerationResponse fetchPokemonGeneration(Generation generation) {
    final String name = generation.getName();
    return pokemonApiRestClient
        .get()
        .uri("/generation/{name}", name)
        .retrieve()
        .requiredBody(GenerationResponse.class);
  }
}
