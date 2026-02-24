package io.github.mangila.spring1.pokemon.internal.integration;

public record PokemonSpeciesResponse(int id, String name, Generation generation) {

  public record Generation(String name, String url) {}
}
