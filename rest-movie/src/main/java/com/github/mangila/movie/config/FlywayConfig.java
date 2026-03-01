package com.github.mangila.movie.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    @Bean
    FlywayMigrationStrategy flywayMigrationStrategy() {
        log.info("Flushing postgres");
        return flyway -> {
            flyway.clean();
            flyway.migrate();
        };
    }

}
