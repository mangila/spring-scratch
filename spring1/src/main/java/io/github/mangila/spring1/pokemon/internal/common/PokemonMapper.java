package io.github.mangila.spring1.pokemon.internal.common;

import io.github.mangila.spring1.pokemon.Pokemon;
import io.github.mangila.spring1.pokemon.internal.data.PokemonEntity;
import io.github.mangila.spring1.pokemon.internal.integration.model.PokemonSpeciesDetail;
import io.github.mangila.spring1.pokemon.internal.rest.model.Generation;
import io.github.mangila.spring1.pokemon.internal.rest.model.PageMetadata;
import io.github.mangila.spring1.pokemon.internal.rest.model.PagedPokemonDto;
import io.github.mangila.spring1.pokemon.internal.rest.model.PokemonDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class PokemonMapper {

  public PagedPokemonDto map(Page<Pokemon> page) {
    final PagedPokemonDto pagedPokemonDto = new PagedPokemonDto();
    final List<PokemonDto> content = page.getContent().stream().map(this::map).toList();
    final PageMetadata pageMetadata =
        new PageMetadata(
            (long) page.getSize(),
            page.getTotalElements(),
            (long) page.getTotalPages(),
            (long) page.getNumber());
    pagedPokemonDto.setContent(content);
    pagedPokemonDto.setPage(pageMetadata);
    return pagedPokemonDto;
  }

  public PokemonDto map(Pokemon pokemon) {
    return new PokemonDto(pokemon.pokedexId(), pokemon.name(), pokemon.generation());
  }

  public Pokemon map(PokemonEntity entity) {
    final Generation generation = map(entity.getGeneration());
    return new Pokemon(entity.getPokedexId(), entity.getName(), generation);
  }

  public Pokemon map(PokemonSpeciesDetail response) {
    final Generation generation = map(response.getGeneration().getName());
    return new Pokemon(response.getId(), response.getName(), generation);
  }

  private Generation map(String value) {
    Generation generation;
    try {
      generation = Generation.fromValue(value);
    } catch (IllegalArgumentException _) {
      generation = Generation.UNKNOWN;
    }
    return generation;
  }
}
