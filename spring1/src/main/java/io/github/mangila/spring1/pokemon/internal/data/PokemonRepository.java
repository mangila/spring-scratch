package io.github.mangila.spring1.pokemon.internal.data;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PokemonRepository extends ListCrudRepository<PokemonEntity, UUID> {

  @Modifying
  void deleteByPokedexId(int id);

  List<PokemonEntity> findAllByOrderByPokedexIdAsc();

  Optional<PokemonEntity> findByPokedexId(int id);
}
