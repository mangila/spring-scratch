package com.github.mangila.movie.config;

import io.hypersistence.utils.spring.repository.BaseJpaRepositoryImpl;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(value = "com.github.mangila.movie.persistence",
		repositoryBaseClass = BaseJpaRepositoryImpl.class)
@EnableJpaAuditing
public class JpaConfig {

}
