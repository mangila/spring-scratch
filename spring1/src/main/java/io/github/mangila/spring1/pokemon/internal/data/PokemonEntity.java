package io.github.mangila.spring1.pokemon.internal.data;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("pokemon")
public class PokemonEntity {

  @Id
  @Column("id")
  private UUID id;

  @Column("pokedex_id")
  private int pokedexId;

  @Column("name")
  private String name;

  @Version
  @Column("version")
  private Long version;

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public int getPokedexId() {
    return pokedexId;
  }

  public Long getVersion() {
    return version;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setPokedexId(int pokedexId) {
    this.pokedexId = pokedexId;
  }

  public void setVersion(Long version) {
    this.version = version;
  }
}
