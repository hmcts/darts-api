package uk.gov.hmcts.darts.common.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableCaching
@Profile("in-memory-caching")
public class SimpleCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        System.out.println("Using in memory caching ...");
        return new ConcurrentMapCacheManager();
    }
}
