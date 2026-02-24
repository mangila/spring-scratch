package io.github.mangila.spring1.pokemon;

import io.github.mangila.spring1.pokemon.internal.common.Generation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record PokemonDto(
    @PositiveOrZero @Max(151) int pokedexId,
    @NotBlank String name,
    @NotNull Generation generation) {}
