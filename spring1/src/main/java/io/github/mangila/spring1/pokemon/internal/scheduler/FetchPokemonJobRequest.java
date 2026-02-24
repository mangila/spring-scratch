package io.github.mangila.spring1.pokemon.internal.scheduler;

import org.jobrunr.jobs.lambdas.JobRequest;

public record FetchPokemonJobRequest(String name, String url) implements JobRequest {

  @Override
  public Class<FetchPokemonJobHandler> getJobRequestHandler() {
    return FetchPokemonJobHandler.class;
  }
}
