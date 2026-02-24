package io.github.mangila.spring1.pokemon;

import io.github.mangila.spring1.pokemon.internal.rest.model.Generation;

public record Pokemon(int pokedexId, String name, Generation generation) {}
