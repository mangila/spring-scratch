package io.github.mangila.spring1.pokemon;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record PokemonDto(@Min(1) @Max(151) int pokedexId, @NotBlank String name) {}
