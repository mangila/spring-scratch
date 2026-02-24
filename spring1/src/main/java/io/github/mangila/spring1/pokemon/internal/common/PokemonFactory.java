package io.github.mangila.spring1.pokemon.internal.common;

import io.github.mangila.spring1.pokemon.PokemonDto;
import io.github.mangila.spring1.pokemon.internal.data.PokemonEntity;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PokemonFactory {

  public PokemonEntity getObject(PokemonDto dto) {
    final PokemonEntity entity = new PokemonEntity();
    entity.setId(UUID.randomUUID());
    entity.setName(dto.name());
    entity.setPokedexId(dto.pokedexId());
    entity.setGeneration(dto.generation().getValue());
    return entity;
  }
}
