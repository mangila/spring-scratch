package io.github.mangila.spring1.pokemon.internal.integration;

public record PokemonResponse(int id, String name) {

  public static final PokemonResponse MISSING_NO = new PokemonResponse(0, "MissingNo");
}
