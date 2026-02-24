package io.github.mangila.spring1.pokemon.internal.config;

import io.github.mangila.spring1.pokemon.internal.rest.model.Generation;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class GenerationConverter implements Converter<String, Generation> {

  @Override
  public Generation convert(String source) {
    return Generation.fromValue(source);
  }
}
