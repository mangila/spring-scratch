package io.github.mangila.spring1.pokemon.internal.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
@ConfigurationProperties(prefix = "app.integration.pokemon-api")
public class PokemonApiConfig {

  private URI url;

  public URI getUrl() {
    return url;
  }

  @Bean
  public RestClient pokemonApiClient() {
    return RestClient.builder()
        .baseUrl(url)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.ACCEPT_ENCODING, "gzip")
        .defaultHeader(HttpHeaders.USER_AGENT, "github/mangila/spring-scratch")
        .build();
  }

  public void setUrl(URI url) {
    this.url = url;
  }
}
