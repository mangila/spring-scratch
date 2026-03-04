package com.github.mangila.app.config;

import io.hypersistence.utils.spring.repository.BaseJpaRepositoryImpl;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Optional;

@Configuration
@EnableJpaRepositories(value = { "com.github.mangila.app.actor.persistence",
		"com.github.mangila.app.director.persistance", "com.github.mangila.app.movie.persistance" },
		repositoryBaseClass = BaseJpaRepositoryImpl.class)
@EnableJpaAuditing(auditorAwareRef = "auditSystemProvider")
public class JpaConfig {

	@Bean("auditSystemProvider")
	AuditorAware<String> auditSystemProvider() {
		return new AuditorAwareImpl();
	}

	private static class AuditorAwareImpl implements AuditorAware<String> {

		@Override
		public @NonNull Optional<String> getCurrentAuditor() {
			return Optional.of("system");
		}

	}

}
