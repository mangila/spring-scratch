package io.github.mangila.spring1.pokemon;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.mangila.spring1.pokemon.internal.common.Generation;
import java.io.IOException;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;

@JsonTest
class PokemonDtoTest {

  @Autowired private JacksonTester<PokemonDto> json;

  @Test
  void fromJson() throws IOException {
    @Language("JSON")
    final String content =
        """
        {
          "pokedexId": 0,
          "name": "missingno",
          "generation": "generation-i"
        }
        """;
    final PokemonDto dto = json.parseObject(content);
    assertThat(dto).isEqualTo(new PokemonDto(0, "missingno", Generation.GENERATION_I));
  }

  @Test
  void toJson() throws IOException {
    final PokemonDto dto = new PokemonDto(0, "missingno", Generation.GENERATION_I);
    final JsonContent<PokemonDto> content = json.write(dto);
    assertThatJson(content.getJson())
        .isObject()
        .containsOnlyKeys("pokedexId", "name", "generation")
        .containsEntry("pokedexId", 0)
        .containsEntry("name", "missingno")
        .containsEntry("generation", "generation-i");
  }
}
