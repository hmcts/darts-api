package uk.gov.hmcts.darts.testutils;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import static uk.gov.hmcts.darts.common.util.ArmRedisConstants.ARM_TOKEN_CACHE_NAME;

@Profile("in-memory-caching")
@TestConfiguration
public class InMemoryTestCache {

    @Bean(name = "armRedisCacheManager")
    public CacheManager armRedisCacheManager() {
        return new ConcurrentMapCacheManager(ARM_TOKEN_CACHE_NAME);
    }

}
