package io.github.mangila.spring1.pokemon.internal.config;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "app.integration.pokemon-api")
@Validated
public class PokeApiConfig {

  @NotNull(message = "Pokemon API URL must not be null") private URI url;

  public URI getUrl() {
    return url;
  }

  public void setUrl(URI url) {
    this.url = url;
  }
}
