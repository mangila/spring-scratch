package io.github.mangila.spring1.pokemon.internal.scheduler;

import io.github.mangila.spring1.pokemon.PokemonDto;
import io.github.mangila.spring1.pokemon.PokemonService;
import io.github.mangila.spring1.pokemon.internal.common.PokemonMapper;
import io.github.mangila.spring1.pokemon.internal.integration.PokemonIntegration;
import io.github.mangila.spring1.pokemon.internal.integration.PokemonResponse;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
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
    final String url = jobRequest.url();
    final PokemonResponse pokemonResponse = pokemonIntegration.fetchPokemonByUrl(url);
    jobContext.logger().info(pokemonResponse.toString());
    final PokemonDto dto = pokemonMapper.map(pokemonResponse);
    pokemonService.createPokemon(dto);
  }
}
