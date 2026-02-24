package io.github.mangila.spring1.pokemon.internal.integration;

import io.github.mangila.spring1.pokemon.internal.config.PokeApiConfig;
import io.github.mangila.spring1.pokemon.internal.integration.client.ApiException;
import io.github.mangila.spring1.pokemon.internal.integration.model.GenerationDetail;
import io.github.mangila.spring1.pokemon.internal.integration.model.PokemonSpeciesDetail;
import io.github.mangila.spring1.pokemon.internal.rest.model.Generation;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled(value = "Send HTTP request to Pokemon API")
@SpringBootTest(
    classes = {PokeApiConfig.class},
    properties = {"app.integration.pokemon-api.url=https://pokeapi.co"})
class PokeApiIntegrationE2e {

  @Autowired private PokeApiIntegration pokeApiIntegration;

  @Test
  void fetchPokemonGeneration() throws ApiException {
    GenerationDetail generationDetail =
        pokeApiIntegration.gamesApi().generationRetrieve(Generation.GENERATION_I.getValue());
    IO.println(generationDetail);
  }

  @Test
  void fetchPokemonSpecies() throws ApiException {
    PokemonSpeciesDetail pokemonSpeciesDetail =
        pokeApiIntegration.pokemonApi().pokemonSpeciesRetrieve("1");
    IO.println(pokemonSpeciesDetail);
  }
}
