package io.github.mangila.spring1.pokemon.internal.data;

import io.github.mangila.spring1.pokemon.PokemonDto;
import io.github.mangila.spring1.pokemon.PokemonService;
import io.github.mangila.spring1.pokemon.internal.common.Generation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class PokemonSeed implements ApplicationRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(PokemonSeed.class);

  private final PokemonService pokemonService;

  public PokemonSeed(PokemonService pokemonService) {
    this.pokemonService = pokemonService;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    final PokemonDto dto = new PokemonDto(0, "missingno", Generation.GENERATION_I);
    LOGGER.info("Seeding: {}", dto.name());
    pokemonService.createPokemon(dto);
  }
}
