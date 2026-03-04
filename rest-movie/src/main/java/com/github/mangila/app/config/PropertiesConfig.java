package com.github.mangila.app.config;

import com.github.mangila.app.actor.properties.ActorProperties;
import com.github.mangila.app.director.properties.DirectorProperties;
import com.github.mangila.app.movie.properties.MovieProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        ActorProperties.class,
        DirectorProperties.class,
        MovieProperties.class})
public class PropertiesConfig {
}
