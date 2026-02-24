package io.github.mangila.spring1.pokemon.internal.common;

import io.github.mangila.spring1.pokemon.PokemonDto;
import io.github.mangila.spring1.pokemon.internal.data.PokemonEntity;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class PokemonFactory implements ObjectProvider<PokemonEntity> {

  @Override
  public PokemonEntity getObject(@Nullable Object... args) throws BeansException {
    final PokemonDto dto = (PokemonDto) args[0];
    final PokemonEntity entity = new PokemonEntity();
    entity.setId(UUID.randomUUID());
    entity.setName(dto.name());
    entity.setPokedexId(dto.pokedexId());
    return entity;
  }
}
