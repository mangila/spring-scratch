package com.github.mangila.movie.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.postgres.PostgresStorageProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JobRunrConfig {

    @Bean
    public StorageProvider storageProvider(HikariDataSource dataSource) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dataSource.getJdbcUrl());
        config.setUsername(dataSource.getUsername());
        config.setPassword(dataSource.getPassword());
        config.setPoolName("movie-jobrunr-hikari-pool");
        config.setAutoCommit(true);
        config.setMaximumPoolSize(10);
        config.setLeakDetectionThreshold(3000L);
        return new PostgresStorageProvider(new HikariDataSource(config));
    }

}
