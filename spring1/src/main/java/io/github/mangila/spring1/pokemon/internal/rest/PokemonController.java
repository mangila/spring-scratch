package io.github.mangila.spring1.pokemon.internal.rest;

import io.github.mangila.spring1.pokemon.PokemonDto;
import io.github.mangila.spring1.pokemon.PokemonService;
import io.github.mangila.spring1.pokemon.internal.common.Generation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/pokemon")
public class PokemonController {

  private final PokemonService pokemonService;

  public PokemonController(PokemonService pokemonService) {
    this.pokemonService = pokemonService;
  }

  @DeleteMapping("{pokedexId}")
  public ResponseEntity<Void> deletePokemonByPokedexId(
      @PathVariable("pokedexId") @Min(1) @Max(151) int id) {
    pokemonService.deletePokemonByPokedexId(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("schedule/{generation}")
  public ResponseEntity<Void> enqueuePokemonGeneration(@PathVariable Generation generation) {
    pokemonService.enqueuePokemonGeneration(generation);
    return ResponseEntity.ok().build();
  }

  @GetMapping
  public List<PokemonDto> findAllPokemon() {
    return pokemonService.findAllPokemon();
  }

  @GetMapping("{pokedexId}")
  public PokemonDto findPokemonByPokedexId(@PathVariable("pokedexId") @Min(1) @Max(151) int id) {
    return pokemonService.findPokemonByPokedexId(id);
  }
}
