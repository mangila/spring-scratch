package io.github.mangila.spring1.pokemon.internal.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record GenerationResponse(
    @JsonProperty("pokemon_species") List<PokemonSpecies> pokemonSpecies) {

  public record PokemonSpecies(String name, String url) {}
}
