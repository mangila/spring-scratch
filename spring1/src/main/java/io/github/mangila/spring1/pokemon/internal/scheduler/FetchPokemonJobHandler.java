package io.github.mangila.spring1.pokemon.internal.scheduler;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodCall;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import io.github.mangila.spring1.pokemon.Pokemon;
import io.github.mangila.spring1.pokemon.PokemonService;
import io.github.mangila.spring1.pokemon.internal.common.PokemonMapper;
import io.github.mangila.spring1.pokemon.internal.integration.PokeApiIntegration;
import io.github.mangila.spring1.pokemon.internal.integration.S3Integration;
import io.github.mangila.spring1.pokemon.internal.integration.client.ApiException;
import io.github.mangila.spring1.pokemon.internal.integration.model.PokemonDetail;
import io.github.mangila.spring1.pokemon.internal.integration.model.PokemonSpeciesDetail;
import io.github.mangila.spring1.pokemon.internal.integration.model.PokemonSpeciesVariety;
import io.github.mangila.spring1.pokemon.internal.rest.PokemonResourceImpl;
import java.net.URI;
import java.util.NoSuchElementException;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import software.amazon.awssdk.core.sync.RequestBody;

@Service
public class FetchPokemonJobHandler implements JobRequestHandler<FetchPokemonJobRequest> {

  private final PokemonMapper pokemonMapper;
  private final PokeApiIntegration pokeApiIntegration;
  private final S3Integration s3Integration;
  private final PokemonService pokemonService;

  public FetchPokemonJobHandler(
      PokemonMapper pokemonMapper,
      PokeApiIntegration pokeApiIntegration,
      S3Integration s3Integration,
      PokemonService pokemonService) {
    this.pokemonMapper = pokemonMapper;
    this.pokeApiIntegration = pokeApiIntegration;
    this.s3Integration = s3Integration;
    this.pokemonService = pokemonService;
  }

  @Override
  public void run(FetchPokemonJobRequest jobRequest) {
    final JobContext jobContext = ThreadLocalJobContext.getJobContext();
    final String name = jobRequest.name();
    if (!isNewPokemon(name)) {
      jobContext.logger().info("Pokemon already exists: %s".formatted(name));
      return;
    }
    try {
      final PokemonSpeciesDetail pokemonSpeciesDetail =
          pokeApiIntegration.pokemonApi().pokemonSpeciesRetrieve(name);
      jobContext.logger().info(pokemonSpeciesDetail.toString());
      final PokemonSpeciesVariety speciesVariety =
          pokemonSpeciesDetail.getVarieties().stream()
              .filter(PokemonSpeciesVariety::getIsDefault)
              .findFirst()
              .orElseThrow(() -> new NoSuchElementException("No default variety found"));
      final String defaultSpecies = speciesVariety.getPokemon().getName();
      final PokemonDetail pokemonDetail =
          pokeApiIntegration.pokemonApi().pokemonRetrieve(defaultSpecies);
      jobContext.logger().info(pokemonDetail.toString());
      final String spriteUrl = pokemonDetail.getSprites().getFrontDefault();
      byte[] spriteBytes = pokeApiIntegration.getPokemonSprite(spriteUrl);
      s3Integration.putObject(name, RequestBody.fromBytes(spriteBytes));
      URI imageUrl = createUriForPokemonImage(name);
      final Pokemon pokemon = pokemonMapper.map(pokemonSpeciesDetail, pokemonDetail, imageUrl);
      pokemonService.createPokemon(pokemon);
    } catch (ApiException e) {
      jobContext.logger().error("Failed to fetch pokemon: %s: %s".formatted(name, e.getMessage()));
      throw new IllegalStateException(e);
    }
  }

  private URI createUriForPokemonImage(String name) {
    return fromMethodCall(
            UriComponentsBuilder.fromPath(""),
            on(PokemonResourceImpl.class).findPokemonImageByName(name))
        .buildAndExpand(name)
        .toUri();
  }

  private boolean isNewPokemon(String name) {
    return pokemonService.findPokemonByName(name).isEmpty();
  }
}
