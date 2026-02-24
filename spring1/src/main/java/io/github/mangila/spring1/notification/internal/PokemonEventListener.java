package io.github.mangila.spring1.notification.internal;

import io.github.mangila.spring1.pokemon.PokemonCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

@Service
public class PokemonEventListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(PokemonEventListener.class);

  @ApplicationModuleListener
  public void handlePokemonCreatedEvent(PokemonCreatedEvent event) {
    LOGGER.info("Received PokemonCreatedEvent for Pokemon: {}", event.name());
  }
}
