package io.github.mangila.spring1.pokemon.internal.integration;

import io.github.mangila.spring1.pokemon.internal.common.Generation;
import io.github.mangila.spring1.pokemon.internal.config.PokemonApiConfig;
import java.net.URI;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;

@Disabled(value = "Send HTTP request to Pokemon API")
@RestClientTest(
    value = {PokemonIntegration.class, PokemonApiConfig.class},
    properties = {"app.integration.pokemon-api.url=https://pokeapi.co/api/v2"})
class PokemonIntegrationE2e {

  @Autowired private PokemonIntegration pokemonIntegration;

  @Test
  void fetchPokemonByUrl() {
    GenerationResponse generationResponse =
        pokemonIntegration.fetchPokemonGeneration(Generation.GENERATION_I);
    IO.print(generationResponse);
  }

  @Test
  void fetchPokemonGeneration() {
    String url = "https://pokeapi.co/api/v2/pokemon-species/1/";
    PokemonSpeciesResponse pokemonSpeciesResponse =
        pokemonIntegration.fetchPokemonByUrl(URI.create(url));
    IO.print(pokemonSpeciesResponse);
  }
}
