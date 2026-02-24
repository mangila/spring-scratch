package io.github.mangila.spring1.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

  @Bean
  public CacheManager cacheManager() {
    SimpleCacheManager cacheManager = new SimpleCacheManager();
    CaffeineCache pokemonCache =
        new CaffeineCache(
            "pokemon",
            Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(1_000)
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .recordStats()
                .build());
    cacheManager.setCaches(List.of(pokemonCache));
    return cacheManager;
  }
}
