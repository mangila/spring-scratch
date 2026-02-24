package io.github.mangila.spring1.pokemon;

import io.github.mangila.spring1.pokemon.internal.common.Generation;
import io.github.mangila.spring1.pokemon.internal.common.PokemonFactory;
import io.github.mangila.spring1.pokemon.internal.common.PokemonMapper;
import io.github.mangila.spring1.pokemon.internal.data.PokemonEntity;
import io.github.mangila.spring1.pokemon.internal.data.PokemonRepository;
import io.github.mangila.spring1.pokemon.internal.integration.GenerationResponse;
import io.github.mangila.spring1.pokemon.internal.integration.PokemonIntegration;
import io.github.mangila.spring1.pokemon.internal.scheduler.FetchPokemonJobRequest;
import io.github.mangila.spring1.pokemon.internal.scheduler.JobRunrScheduler;
import java.util.List;
import java.util.stream.Stream;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PokemonService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PokemonService.class);

  private final CaffeineCache caffeineCache;

  private final PokemonRepository pokemonRepository;

  private final PokemonMapper pokemonMapper;

  private final PokemonFactory pokemonFactory;

  private final PokemonIntegration pokemonIntegration;

  private final JobRunrScheduler jobRunrScheduler;

  private final ApplicationEventPublisher eventPublisher;

  public PokemonService(
      CacheManager cacheManager,
      PokemonRepository pokemonRepository,
      PokemonMapper pokemonMapper,
      PokemonFactory pokemonFactory,
      PokemonIntegration pokemonIntegration,
      JobRunrScheduler jobRunrScheduler,
      ApplicationEventPublisher eventPublisher) {
    this.caffeineCache = (CaffeineCache) cacheManager.getCache("pokemon");
    this.pokemonRepository = pokemonRepository;
    this.pokemonMapper = pokemonMapper;
    this.pokemonFactory = pokemonFactory;
    this.pokemonIntegration = pokemonIntegration;
    this.jobRunrScheduler = jobRunrScheduler;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public PokemonDto createPokemon(PokemonDto pokemonDto) {
    final PokemonEntity entity = pokemonFactory.getObject(pokemonDto);
    final PokemonEntity saved = pokemonRepository.save(entity);
    final PokemonDto result = pokemonMapper.map(saved);
    caffeineCache.put(result.pokedexId(), result);
    eventPublisher.publishEvent(new PokemonCreatedEvent(result.name()));
    return result;
  }

  @Transactional
  public void deletePokemonByPokedexId(int id) {
    pokemonRepository.deleteByPokedexId(id);
    caffeineCache.evict(id);
  }

  public void enqueuePokemonGeneration(Generation generation) {
    final GenerationResponse response = pokemonIntegration.fetchPokemonGeneration(generation);
    final Stream<JobRequest> jobRequests =
        response.pokemonSpecies().stream()
            .map(pokemonSpecies -> new FetchPokemonJobRequest(pokemonSpecies.url()));
    jobRunrScheduler.enqueue(jobRequests);
  }

  public List<PokemonDto> findAllPokemon() {
    return pokemonRepository.findAllByOrderByPokedexIdAsc().stream()
        .map(pokemonMapper::map)
        .toList();
  }

  public PokemonDto findPokemonByPokedexId(int id) {
    return caffeineCache.get(
        id,
        () -> {
          LOGGER.info("Cache MISS: id={}", id);
          return pokemonRepository.findByPokedexId(id).map(pokemonMapper::map).orElseThrow();
        });
  }
}
