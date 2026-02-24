package io.github.mangila.spring1.pokemon.internal.rest;

import io.github.mangila.spring1.pokemon.PokemonDto;
import io.github.mangila.spring1.pokemon.PokemonService;
import io.github.mangila.spring1.pokemon.internal.common.Generation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/pokemon")
public class PokemonController {

  private final PokemonService pokemonService;

  public PokemonController(PokemonService pokemonService) {
    this.pokemonService = pokemonService;
  }

  @DeleteMapping("id/{pokedexId}")
  public ResponseEntity<Void> deletePokemonByPokedexId(
      @PathVariable("pokedexId") @PositiveOrZero @Max(151) int id) {
    pokemonService.deletePokemonByPokedexId(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("schedule/{generation}")
  public ResponseEntity<Void> enqueuePokemonGeneration(@PathVariable Generation generation) {
    pokemonService.enqueuePokemonGeneration(generation);
    return ResponseEntity.ok().build();
  }

  @GetMapping
  public PagedModel<PokemonDto> findAllPokemon(
      @RequestParam("page") @PositiveOrZero int pageNumber,
      @RequestParam("size") @Positive @Max(100) int size) {
    final Pageable pageable = PageRequest.of(pageNumber, size, Sort.by("pokedexId").ascending());
    final Page<PokemonDto> page = pokemonService.findAllPokemon(pageable);
    return new PagedModel<>(page);
  }

  @GetMapping("name/{pokemonName}")
  public PokemonDto findPokemonByName(
      @PathVariable("pokemonName") @NotBlank @Size(min = 2, max = 100) String name) {
    return pokemonService.findPokemonByName(name);
  }

  @GetMapping("id/{pokedexId}")
  public PokemonDto findPokemonByPokedexId(
      @PathVariable("pokedexId") @PositiveOrZero @Max(151) int id) {
    return pokemonService.findPokemonByPokedexId(id);
  }
}
