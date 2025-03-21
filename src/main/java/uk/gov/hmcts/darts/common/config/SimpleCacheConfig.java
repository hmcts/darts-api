package uk.gov.hmcts.darts.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableCaching
@Profile("in-memory-caching")
@Slf4j
public class SimpleCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        log.debug("Using in memory caching ...");
        return new ConcurrentMapCacheManager();
    }
}
