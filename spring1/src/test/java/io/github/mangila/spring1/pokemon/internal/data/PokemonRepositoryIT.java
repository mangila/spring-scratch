package io.github.mangila.spring1.pokemon.internal.data;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mangila.spring1.TestcontainersConfiguration;
import io.github.mangila.spring1.pokemon.internal.common.Generation;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Import(TestcontainersConfiguration.class)
@DataJdbcTest
class PokemonRepositoryIT {

  @Autowired private PokemonRepository repository;

  private final int pokedexId = 0;
  private final String name = "missingno";

  @AfterEach
  void afterEach() {
    repository.deleteAll();
  }

  @BeforeEach
  void beforeEach() {
    PokemonEntity entity = new PokemonEntity();
    entity.setId(UUID.randomUUID());
    entity.setPokedexId(pokedexId);
    entity.setName(name);
    entity.setGeneration(Generation.GENERATION_I.getValue());
    repository.save(entity);
  }

  @Test
  void findAllPage() {
    Pageable pageable = Pageable.ofSize(5);
    Page<PokemonEntity> page = repository.findAll(pageable);
    assertThat(page).isNotEmpty().hasSize(1);
  }

  @Test
  void findAndDeleteByPokedexId() {
    Optional<PokemonEntity> entity = repository.findByPokedexId(pokedexId);
    assertThat(entity).isPresent();
    repository.deleteByPokedexId(pokedexId);
    entity = repository.findByPokedexId(pokedexId);
    assertThat(entity).isEmpty();
  }

  @Test
  void findByName() {
    Optional<PokemonEntity> entity = repository.findByNameIgnoreCase(name);
    assertThat(entity).isPresent();
  }
}
