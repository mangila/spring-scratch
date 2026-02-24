package io.github.mangila.spring1.pokemon.internal.config;

import io.github.mangila.spring1.common.SecretProperty;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "app.integration.rust-fs")
@Validated
public class RustFsConfig {

  @NotNull(message = "Rust FS API URL must not be null") private URI url;

  @NotNull(message = "Rust FS API bucket must not be null") private String bucket;

  @NotNull(message = "Rust FS API username must not be null") private String username;

  @NotNull(message = "Rust FS API password must not be null") private SecretProperty<String> password;

  public String getBucket() {
    return bucket;
  }

  public SecretProperty<String> getPassword() {
    return password;
  }

  public URI getUrl() {
    return url;
  }

  public String getUsername() {
    return username;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public void setPassword(String password) {
    this.password = new SecretProperty<>(password);
  }

  public void setUrl(URI url) {
    this.url = url;
  }

  public void setUsername(String username) {
    this.username = username;
  }
}
