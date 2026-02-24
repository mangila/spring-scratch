package io.github.mangila.spring1.pokemon.internal.scheduler;

import io.github.mangila.spring1.pokemon.PokemonDto;
import io.github.mangila.spring1.pokemon.PokemonService;
import io.github.mangila.spring1.pokemon.internal.common.PokemonMapper;
import io.github.mangila.spring1.pokemon.internal.integration.PokemonIntegration;
import io.github.mangila.spring1.pokemon.internal.integration.PokemonSpeciesResponse;
import java.net.URI;
import java.util.NoSuchElementException;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;

@Service
public class FetchPokemonJobHandler implements JobRequestHandler<FetchPokemonJobRequest> {

  private final PokemonMapper pokemonMapper;
  private final PokemonIntegration pokemonIntegration;
  private final PokemonService pokemonService;

  public FetchPokemonJobHandler(
      PokemonMapper pokemonMapper,
      PokemonIntegration pokemonIntegration,
      PokemonService pokemonService) {
    this.pokemonMapper = pokemonMapper;
    this.pokemonIntegration = pokemonIntegration;
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
    final String url = jobRequest.url();
    final URI pokemonUrl = parseUrl(url);
    if (pokemonUrl == null) {
      jobContext.logger().error("Not valid formatted URL: %s".formatted(url));
      return;
    }
    final PokemonSpeciesResponse pokemonSpeciesResponse =
        pokemonIntegration.fetchPokemonByUrl(pokemonUrl);
    jobContext.logger().info(pokemonSpeciesResponse.toString());
    final PokemonDto dto = pokemonMapper.map(pokemonSpeciesResponse);
    pokemonService.createPokemon(dto);
  }

  private boolean isNewPokemon(String name) {
    try {
      pokemonService.findPokemonByName(name);
    } catch (Cache.ValueRetrievalException e) {
      if (e.getCause() instanceof NoSuchElementException) {
        return true;
      }
    }
    return false;
  }

  private @Nullable URI parseUrl(String url) {
    try {
      return URI.create(url);
    } catch (IllegalArgumentException _) {
      return null;
    }
  }
}
