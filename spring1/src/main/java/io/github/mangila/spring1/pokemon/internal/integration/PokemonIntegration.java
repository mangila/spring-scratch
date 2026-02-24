package io.github.mangila.spring1.pokemon.internal.integration;

import io.github.mangila.spring1.pokemon.internal.common.Generation;
import java.net.URI;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PokemonIntegration {

  private final RestClient pokemonApiRestClient;

  public PokemonIntegration(@Qualifier("pokemonApiClient") RestClient client) {
    this.pokemonApiRestClient = client;
  }

  public PokemonSpeciesResponse fetchPokemonByUrl(URI url) {
    return pokemonApiRestClient
        .mutate()
        .baseUrl(url)
        .build()
        .get()
        .retrieve()
        .requiredBody(PokemonSpeciesResponse.class);
  }

  public GenerationResponse fetchPokemonGeneration(Generation generation) {
    return pokemonApiRestClient
        .get()
        .uri("/generation/{generationName}", generation.getValue())
        .retrieve()
        .requiredBody(GenerationResponse.class);
  }
}
