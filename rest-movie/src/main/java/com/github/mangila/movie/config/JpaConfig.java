package com.github.mangila.movie.config;

import io.hypersistence.utils.spring.repository.BaseJpaRepositoryImpl;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(
        value = "com.github.mangila.movie.persistence",
        repositoryBaseClass = BaseJpaRepositoryImpl.class
)
public class JpaConfig {
}
