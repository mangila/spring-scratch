package com.github.mangila.app.config;

import com.github.mangila.app.actor.properties.*;
import com.github.mangila.app.director.properties.*;
import com.github.mangila.app.movie.properties.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        // Actor
        ActorProperties.class,
        ActorOutboxRelayProperties.class,
        ActorOutboxMonitorProperties.class,
        ActorOutboxPurgeProperties.class,
        ActorOutboxRecoverProperties.class,
        // Director
        DirectorProperties.class,
        DirectorOutboxRelayProperties.class,
        DirectorOutboxMonitorProperties.class,
        DirectorOutboxPurgeProperties.class,
        DirectorOutboxRecoverProperties.class,
        // Movie
        MovieProperties.class,
        MovieOutboxRelayProperties.class,
        MovieOutboxMonitorProperties.class,
        MovieOutboxPurgeProperties.class,
        MovieOutboxRecoverProperties.class
})
public class PropertiesConfig {

}
