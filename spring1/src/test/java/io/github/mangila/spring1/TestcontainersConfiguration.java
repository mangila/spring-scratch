package io.github.mangila.spring1;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  private static final PostgreSQLContainer POSTGRES_CONTAINER =
      new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"))
          .withDatabaseName("mydatabase")
          .withUsername("myuser")
          .withPassword("mypassword")
          .withReuse(true);

  private static final GenericContainer<?> RUSTFS_CONTAINER =
      new GenericContainer<>(DockerImageName.parse("rustfs/rustfs:latest"))
          .withExposedPorts(9000, 9001)
          .withEnv("RUSTFS_ACCESS_KEY", "rustfsadmin")
          .withEnv("RUSTFS_SECRET_KEY", "rustfsadmin123")
          .withReuse(true);

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgresContainer() {
    return POSTGRES_CONTAINER;
  }

  @Bean(initMethod = "start")
  GenericContainer<?> rustfsContainer() {
    return RUSTFS_CONTAINER;
  }

  @Bean
  DynamicPropertyRegistrar rustfsProperties() {
    return registry -> {
      registry.add(
          "app.integration.rust-fs.url",
          () ->
              String.format(
                  "http://%s:%d",
                  RUSTFS_CONTAINER.getHost(), RUSTFS_CONTAINER.getMappedPort(9000)));
      registry.add("app.integration.rust-fs.username", () -> "rustfsadmin");
      registry.add("app.integration.rust-fs.password", () -> "rustfsadmin123");
    };
  }
}
