package uk.gov.hmcts.darts.common.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager inMemoryCacheManager() {
        return new ConcurrentMapCacheManager();
    }
}
