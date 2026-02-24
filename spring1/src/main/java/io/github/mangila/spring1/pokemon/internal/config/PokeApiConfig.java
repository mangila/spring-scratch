package io.github.mangila.spring1.pokemon.internal.config;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.http.client.InetAddressFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "app.integration.pokemon-api")
@Validated
public class PokeApiConfig {

  @NotNull(message = "Pokemon API URL must not be null") private URI url;

  @NotNull(message = "Pokemon API sprites URL must not be null") private URI spritesUrl;

  public URI getSpritesUrl() {
    return spritesUrl;
  }

  public URI getUrl() {
    return url;
  }

  @Bean
  public InetAddressFilter inetAddressFilter() {
    return InetAddressFilter.externalAddresses();
  }

  public void setSpritesUrl(URI spritesUrl) {
    this.spritesUrl = spritesUrl;
  }

  public void setUrl(URI url) {
    this.url = url;
  }
}
