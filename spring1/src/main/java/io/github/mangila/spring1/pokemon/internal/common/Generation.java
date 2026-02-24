package io.github.mangila.spring1.pokemon.internal.common;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Generation {
  GENERATION_I("generation-i");

  public static Generation fromValue(String value) {
    for (Generation gen : values()) {
      if (gen.value.equalsIgnoreCase(value)) {
        return gen;
      }
    }
    throw new IllegalArgumentException("Unknown generation: " + value);
  }

  private final String value;

  Generation(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }
}
