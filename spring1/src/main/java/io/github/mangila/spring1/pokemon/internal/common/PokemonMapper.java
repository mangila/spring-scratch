package io.github.mangila.spring1.pokemon.internal.common;

import io.github.mangila.spring1.pokemon.PokemonDto;
import io.github.mangila.spring1.pokemon.internal.data.PokemonEntity;
import io.github.mangila.spring1.pokemon.internal.integration.PokemonResponse;
import org.springframework.stereotype.Component;

@Component
public class PokemonMapper {

  public PokemonDto map(PokemonEntity entity) {
    return new PokemonDto(entity.getPokedexId(), entity.getName());
  }

  public PokemonDto map(PokemonResponse response) {
    return new PokemonDto(response.id(), response.name());
  }
}
