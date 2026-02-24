package io.github.mangila.spring1.pokemon.internal.common;

public enum Generation {
  GENERATION_I("generation-i"),
  GENERATION_II("generation-ii"),
  ;

  private final String name;

  Generation(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
