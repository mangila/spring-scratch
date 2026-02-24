package io.github.mangila.spring1.pokemon;

import io.github.mangila.spring1.pokemon.internal.common.PokemonFactory;
import io.github.mangila.spring1.pokemon.internal.common.PokemonMapper;
import io.github.mangila.spring1.pokemon.internal.data.PokemonEntity;
import io.github.mangila.spring1.pokemon.internal.data.PokemonRepository;
import io.github.mangila.spring1.pokemon.internal.integration.PokeApiIntegration;
import io.github.mangila.spring1.pokemon.internal.integration.client.ApiException;
import io.github.mangila.spring1.pokemon.internal.integration.model.GenerationDetail;
import io.github.mangila.spring1.pokemon.internal.rest.model.Generation;
import io.github.mangila.spring1.pokemon.internal.scheduler.FetchPokemonJobRequest;
import io.github.mangila.spring1.pokemon.internal.scheduler.JobRunrScheduler;
import java.util.Optional;
import java.util.stream.Stream;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PokemonService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PokemonService.class);

  private final PokemonRepository pokemonRepository;

  private final PokemonMapper pokemonMapper;

  private final PokemonFactory pokemonFactory;

  private final PokeApiIntegration pokeApiIntegration;

  private final JobRunrScheduler jobRunrScheduler;

  private final ApplicationEventPublisher eventPublisher;

  public PokemonService(
      PokemonRepository pokemonRepository,
      PokemonMapper pokemonMapper,
      PokemonFactory pokemonFactory,
      PokeApiIntegration pokeApiIntegration,
      JobRunrScheduler jobRunrScheduler,
      ApplicationEventPublisher eventPublisher) {
    this.pokemonRepository = pokemonRepository;
    this.pokemonMapper = pokemonMapper;
    this.pokemonFactory = pokemonFactory;
    this.pokeApiIntegration = pokeApiIntegration;
    this.jobRunrScheduler = jobRunrScheduler;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public Pokemon createPokemon(Pokemon pokemon) {
    final PokemonEntity entity = pokemonFactory.getObject(pokemon);
    final PokemonEntity saved = pokemonRepository.save(entity);
    final Pokemon result = pokemonMapper.map(saved);
    eventPublisher.publishEvent(new PokemonCreatedEvent(result.name()));
    return result;
  }

  @Transactional
  public boolean deletePokemonByPokedexId(int id) {
    return pokemonRepository.deleteByPokedexId(id);
  }

  public void enqueuePokemonGeneration(Generation generation) {
    try {
      final GenerationDetail response =
          pokeApiIntegration.gamesApi().generationRetrieve(generation.getValue());
      final Stream<JobRequest> jobRequests =
          response.getPokemonSpecies().stream()
              .map(
                  pokemonSpecies ->
                      new FetchPokemonJobRequest(
                          pokemonSpecies.getName(), pokemonSpecies.getUrl().toString()));
      jobRunrScheduler.enqueue(jobRequests);
    } catch (ApiException e) {
      LOGGER.error("Failed to fetch generation: {}", generation, e);
      throw new RuntimeException("Error fetching pokemon generation", e);
    }
  }

  public Page<Pokemon> findAllPokemon(Pageable pageable) {
    return pokemonRepository.findAll(pageable).map(pokemonMapper::map);
  }

  public Optional<Pokemon> findPokemonByName(String name) {
    return pokemonRepository.findByNameIgnoreCase(name).map(pokemonMapper::map);
  }

  public Optional<Pokemon> findPokemonByPokedexId(int id) {
    return pokemonRepository.findByPokedexId(id).map(pokemonMapper::map);
  }
}
