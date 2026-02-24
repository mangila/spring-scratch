package io.github.mangila.spring1;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ArchitectureTest {

  private final ApplicationModules modules = ApplicationModules.of(Application.class);

  @Test
  void printModuleOverview() {
    assertThat(modules).isNotNull();
    modules.forEach(System.out::println);
  }

  @Test
  void verifyModuleStructure() {
    modules.verify();
  }
}
