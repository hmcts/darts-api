package uk.gov.hmcts.darts.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

import static uk.gov.hmcts.darts.common.util.RedisConstants.ARM_TOKEN_CACHE;

@Configuration
@EnableCaching
@Profile("!in-memory-caching")
public class ArmTokenCacheConfig {

    @Value("${darts.storage.arm.arm-token-cache-expiry}")
    private Duration armTokenCacheExpiry;

    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        var keySerializer = new StringRedisSerializer();
        var valueSerializer = new StringRedisSerializer(); // token is a String

        RedisCacheConfiguration baseConfig = RedisCacheConfiguration
            .defaultCacheConfig()
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
            .disableCachingNullValues()
            .prefixCacheNameWith("darts:")
            .entryTtl(armTokenCacheExpiry);

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(baseConfig)
            .withCacheConfiguration(ARM_TOKEN_CACHE, baseConfig)
            .build();
    }

}
