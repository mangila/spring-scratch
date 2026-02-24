package io.github.mangila.spring1.pokemon.internal.common;

import io.github.mangila.spring1.pokemon.PokemonDto;
import io.github.mangila.spring1.pokemon.internal.data.PokemonEntity;
import io.github.mangila.spring1.pokemon.internal.integration.PokemonSpeciesResponse;
import org.springframework.stereotype.Component;

@Component
public class PokemonMapper {

  public PokemonDto map(PokemonEntity entity) {
    final Generation generation = Generation.fromValue(entity.getGeneration());
    return new PokemonDto(entity.getPokedexId(), entity.getName(), generation);
  }

  public PokemonDto map(PokemonSpeciesResponse response) {
    final Generation generation = Generation.fromValue(response.generation().name());
    return new PokemonDto(response.id(), response.name(), generation);
  }
}
