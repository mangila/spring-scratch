package io.github.mangila.spring1.pokemon.internal.data;

import io.github.mangila.spring1.pokemon.Pokemon;
import io.github.mangila.spring1.pokemon.PokemonService;
import io.github.mangila.spring1.pokemon.internal.integration.S3Integration;
import io.github.mangila.spring1.pokemon.internal.rest.model.Generation;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;

@Component
@Profile("prod")
@Order(value = 2)
public class PokemonSeed implements ApplicationRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(PokemonSeed.class);

  private final PokemonService pokemonService;
  private final S3Integration s3Integration;

  public PokemonSeed(PokemonService pokemonService, S3Integration s3Integration) {
    this.pokemonService = pokemonService;
    this.s3Integration = s3Integration;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    final String name = "missingno";
    final Pokemon pokemon =
        new Pokemon(
            0, name, URI.create("/api/v1/image/%s".formatted(name)), Generation.GENERATION_I);
    if (!s3Integration.bucketExists()) {
      throw new IllegalStateException("Bucket does not exist");
    }
    s3Integration.putObject(name, RequestBody.fromBytes(new byte[0]));
    LOGGER.info("Seeding: {}", pokemon.name());
    pokemonService.createPokemon(pokemon);
  }
}
