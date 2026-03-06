package com.github.mangila.app.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JobRunrConfig {

    @Bean
    JsonMapper jsonMapper() {
        return new JacksonJsonMapper();
    }

    @Bean
    JobMapper jobMapper(JsonMapper jsonMapper) {
        return new JobMapper(jsonMapper);
    }

    @Bean
    public StorageProvider storageProvider(HikariDataSource dataSource, JobMapper jobMapper) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dataSource.getJdbcUrl());
        config.setUsername(dataSource.getUsername());
        config.setPassword(dataSource.getPassword());
        config.setPoolName("jobrunr-hikari-pool");
        config.setAutoCommit(true);
        config.setMaximumPoolSize(10);
        config.setLeakDetectionThreshold(3000L);
        var provider = SqlStorageProviderFactory.using(new HikariDataSource(config));
        provider.setJobMapper(jobMapper);
        return provider;
    }

}
