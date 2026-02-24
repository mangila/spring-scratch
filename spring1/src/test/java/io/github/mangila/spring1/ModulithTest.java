package io.github.mangila.spring1;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModulithTest {

  private final ApplicationModules modules = ApplicationModules.of(Application.class);

  @Test
  void printModuleOverview() {
    assertThat(modules).isNotNull();
    modules.forEach(IO::println);
  }

  @Test
  void verifyModuleStructure() {
    modules.verify();
  }

  @Test
  void writeDocumentationSnippets() {
    new Documenter(modules).writeDocumentation();
  }
}
