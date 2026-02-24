package io.github.mangila.spring1.pokemon.internal.rest;

import io.github.mangila.spring1.pokemon.internal.rest.api.PokemonResource;
import io.github.mangila.spring1.pokemon.internal.rest.model.Generation;
import io.github.mangila.spring1.pokemon.internal.rest.model.PagedPokemonDto;
import io.github.mangila.spring1.pokemon.internal.rest.model.PokemonDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PokemonResourceImpl implements PokemonResource {

  private final PokemonRestService pokemonRestService;

  public PokemonResourceImpl(PokemonRestService pokemonRestService) {
    this.pokemonRestService = pokemonRestService;
  }

  @Override
  public ResponseEntity<Void> deletePokemonByPokedexId(Integer pokedexId) {
    pokemonRestService.deletePokemonByPokedexId(pokedexId);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> enqueuePokemonGeneration(Generation generation) {
    pokemonRestService.enqueuePokemonGeneration(generation);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<PagedPokemonDto> findAllPokemon(Integer page, Integer size) {
    final PagedPokemonDto dto = pokemonRestService.findAllPokemon(page, size);
    return ResponseEntity.ok(dto);
  }

  @Override
  public ResponseEntity<PokemonDto> findPokemonByName(String pokemonName) {
    final PokemonDto dto = pokemonRestService.findPokemonByName(pokemonName);
    return ResponseEntity.ok(dto);
  }

  @Override
  public ResponseEntity<PokemonDto> findPokemonByPokedexId(Integer pokedexId) {
    final PokemonDto dto = pokemonRestService.findPokemonByPokedexId(pokedexId);
    return ResponseEntity.ok(dto);
  }
}
