package com.github.mangila.movie.config;

import com.github.mangila.movie.properties.OutboxProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({OutboxProperties.class})
public class PropertiesConfig {
}
