package io.github.mangila.spring1.pokemon.internal.rest;

import io.github.mangila.spring1.pokemon.Pokemon;
import io.github.mangila.spring1.pokemon.PokemonService;
import io.github.mangila.spring1.pokemon.internal.common.PokemonMapper;
import io.github.mangila.spring1.pokemon.internal.integration.S3Integration;
import io.github.mangila.spring1.pokemon.internal.rest.model.Generation;
import io.github.mangila.spring1.pokemon.internal.rest.model.PagedPokemonDto;
import io.github.mangila.spring1.pokemon.internal.rest.model.PokemonDto;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class PokemonRestService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PokemonRestService.class);

  private final PokemonService pokemonService;
  private final PokemonMapper pokemonMapper;
  private final CaffeineCache caffeineCache;
  private final S3Integration s3Integration;

  public PokemonRestService(
      PokemonService pokemonService,
      PokemonMapper pokemonMapper,
      CacheManager cacheManager,
      S3Integration s3Integration) {
    this.pokemonService = pokemonService;
    this.pokemonMapper = pokemonMapper;
    this.caffeineCache = (CaffeineCache) cacheManager.getCache("pokemon");
    this.s3Integration = s3Integration;
  }

  public void deletePokemonByPokedexId(int pokedexId) {
    final boolean deleted = pokemonService.deletePokemonByPokedexId(pokedexId);
    if (!deleted) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
    }
    caffeineCache.evict(pokedexId);
  }

  public void enqueuePokemonGeneration(Generation generation) {
    try {
      pokemonService.enqueuePokemonGeneration(generation);
    } catch (Exception e) {
      LOGGER.error("Failed to fetch generation: {}", generation, e);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching pokemon generation", e);
    }
  }

  public PagedPokemonDto findAllPokemon(Integer page, Integer size) {
    final Pageable pageable = PageRequest.of(page, size, Sort.by("pokedexId").ascending());
    final Page<Pokemon> resultPage = pokemonService.findAllPokemon(pageable);
    return pokemonMapper.map(resultPage);
  }

  public PokemonDto findPokemonByName(String name) {
    try {
      final Pokemon pokemon =
          caffeineCache.get(
              name,
              () -> {
                LOGGER.info("Cache MISS: name={}", name);
                return pokemonService.findPokemonByName(name).orElseThrow();
              });
      if (pokemon == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
      }
      return pokemonMapper.map(pokemon);
    } catch (Cache.ValueRetrievalException e) {
      if (e.getCause() instanceof NoSuchElementException) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
      }
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching pokemon");
    }
  }

  public PokemonDto findPokemonByPokedexId(int id) {
    try {
      final Pokemon pokemon =
          caffeineCache.get(
              id,
              () -> {
                LOGGER.info("Cache MISS: id={}", id);
                return pokemonService.findPokemonByPokedexId(id).orElseThrow();
              });
      if (pokemon == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
      }
      return pokemonMapper.map(pokemon);
    } catch (Cache.ValueRetrievalException e) {
      if (e.getCause() instanceof NoSuchElementException) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
      }
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching pokemon");
    }
  }

  public byte[] findPokemonImageByName(String name) {
    try {
      final byte[] imageBytes =
          caffeineCache.get(
              "imageUrl:" + name,
              () -> {
                LOGGER.info("Cache MISS: imageUrl name={}", name);
                try {
                  return s3Integration.getObject(name);
                } catch (NoSuchKeyException e) {
                  throw new NoSuchElementException("Image not found for pokemon: " + name);
                } catch (S3Exception e) {
                  if (e.statusCode() == 404) {
                    throw new NoSuchElementException("Image not found for pokemon: " + name);
                  }
                  throw e;
                }
              });
      if (imageBytes == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
      }
      return imageBytes;
    } catch (Cache.ValueRetrievalException e) {
      if (e.getCause() instanceof NoSuchElementException) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
      }
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching pokemon imageUrl");
    }
  }
}
