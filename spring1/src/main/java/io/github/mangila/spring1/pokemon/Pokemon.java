package io.github.mangila.spring1.pokemon;

import io.github.mangila.spring1.pokemon.internal.rest.model.Generation;
import java.net.URI;

public record Pokemon(int pokedexId, String name, URI imageUrl, Generation generation) {}
