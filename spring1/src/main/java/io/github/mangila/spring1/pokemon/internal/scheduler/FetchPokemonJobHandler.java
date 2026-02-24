package io.github.mangila.spring1.pokemon.internal.scheduler;

import io.github.mangila.spring1.pokemon.Pokemon;
import io.github.mangila.spring1.pokemon.PokemonService;
import io.github.mangila.spring1.pokemon.internal.common.PokemonMapper;
import io.github.mangila.spring1.pokemon.internal.integration.PokeApiIntegration;
import io.github.mangila.spring1.pokemon.internal.integration.client.ApiException;
import io.github.mangila.spring1.pokemon.internal.integration.model.PokemonSpeciesDetail;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.springframework.stereotype.Service;

@Service
public class FetchPokemonJobHandler implements JobRequestHandler<FetchPokemonJobRequest> {

  private final PokemonMapper pokemonMapper;
  private final PokeApiIntegration pokeApiIntegration;
  private final PokemonService pokemonService;

  public FetchPokemonJobHandler(
      PokemonMapper pokemonMapper,
      PokeApiIntegration pokeApiIntegration,
      PokemonService pokemonService) {
    this.pokemonMapper = pokemonMapper;
    this.pokeApiIntegration = pokeApiIntegration;
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
      final PokemonSpeciesDetail pokemonSpeciesResponse =
          pokeApiIntegration.pokemonApi().pokemonSpeciesRetrieve(name);
      jobContext.logger().info(pokemonSpeciesResponse.toString());
      final Pokemon pokemon = pokemonMapper.map(pokemonSpeciesResponse);
      pokemonService.createPokemon(pokemon);
    } catch (ApiException e) {
      jobContext
          .logger()
          .error("Failed to fetch pokemon species %s: %s".formatted(name, e.getMessage()));
      throw new RuntimeException(e);
    }
  }

  private boolean isNewPokemon(String name) {
    return pokemonService.findPokemonByName(name).isEmpty();
  }
}
