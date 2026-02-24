package io.github.mangila.spring1.pokemon.internal.common;

import io.github.mangila.spring1.pokemon.Pokemon;
import io.github.mangila.spring1.pokemon.internal.data.PokemonEntity;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PokemonFactory {

  public PokemonEntity getObject(Pokemon pokemon) {
    final PokemonEntity entity = new PokemonEntity();
    entity.setId(UUID.randomUUID());
    entity.setName(pokemon.name());
    entity.setPokedexId(pokemon.pokedexId());
    entity.setGeneration(pokemon.generation().getValue());
    return entity;
  }
}
