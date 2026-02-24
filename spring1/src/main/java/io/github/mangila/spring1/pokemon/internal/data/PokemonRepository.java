package io.github.mangila.spring1.pokemon.internal.data;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PokemonRepository extends ListCrudRepository<PokemonEntity, UUID> {

  @Modifying
  void deleteByPokedexId(int id);

  Page<PokemonEntity> findAll(Pageable pageable);

  Optional<PokemonEntity> findByNameIgnoreCase(String name);

  Optional<PokemonEntity> findByPokedexId(int id);
}
