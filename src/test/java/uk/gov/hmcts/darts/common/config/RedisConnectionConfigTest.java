package uk.gov.hmcts.darts.common.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedisConnectionConfigTest {

    @Test
    void createsRedisPropertiesFromConnectionStringWithUsername() {
        var redisConnectionString = "redis://some-user:some-access-key@some-host:6379?key=value";
        var redisProperties = RedisConnectionConfig.redisConnectionPropertiesFrom(redisConnectionString);

        assertThat(redisProperties.host()).isEqualTo("some-host");
        assertThat(redisProperties.port()).isEqualTo(6379);
        assertThat(redisProperties.password()).isEqualTo("some-access-key");
        assertThat(redisProperties.username()).isEqualTo("some-user");
    }

    @Test
    void createsRedisPropertiesFromConnectionStringWithEmptyUsername() {
        var redisConnectionString = "redis://:some-access-key@some-host:6379?key=value";
        var redisProperties = RedisConnectionConfig.redisConnectionPropertiesFrom(redisConnectionString);

        assertThat(redisProperties.host()).isEqualTo("some-host");
        assertThat(redisProperties.port()).isEqualTo(6379);
        assertThat(redisProperties.password()).isEqualTo("some-access-key");
        assertThat(redisProperties.username()).isEmpty();
    }
}
