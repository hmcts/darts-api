package uk.gov.hmcts.darts.common.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedisConnectionConfigTest {

    @Test
    void createsRedisPropertiesFromConnectionStringWithPasswordAndUsername() {
        var redisConnectionString = "redis://some-user:some-access-key@some-host:6379?key=value";
        var redisProperties = RedisConnectionConfig.redisConnectionPropertiesFrom(redisConnectionString);

        assertThat(redisProperties.host()).isEqualTo("some-host");
        assertThat(redisProperties.port()).isEqualTo(6379);
        assertThat(redisProperties.password()).isEqualTo("some-access-key".toCharArray());
        assertThat(redisProperties.username()).isEqualTo("some-user");
    }

    @Test
    void createsRedisPropertiesFromConnectionStringWithEmptyUsername() {
        var redisConnectionString = "redis://:some-access-key@some-host:6379?key=value";
        var redisProperties = RedisConnectionConfig.redisConnectionPropertiesFrom(redisConnectionString);

        assertThat(redisProperties.host()).isEqualTo("some-host");
        assertThat(redisProperties.port()).isEqualTo(6379);
        assertThat(redisProperties.password()).isEqualTo("some-access-key".toCharArray());
        assertThat(redisProperties.username()).isNullOrEmpty();
    }

    @Test
    void createsRedisPropertiesFromConnectionStringWithEmptyUsernameAndPassword() {
        var redisConnectionString = "redis://some-host:6379";
        var redisProperties = RedisConnectionConfig.redisConnectionPropertiesFrom(redisConnectionString);

        assertThat(redisProperties.host()).isEqualTo("some-host");
        assertThat(redisProperties.port()).isEqualTo(6379);
        assertThat(redisProperties.password()).isNullOrEmpty();
        assertThat(redisProperties.username()).isNullOrEmpty();
    }

    @Test
    void createsRedisPropertiesFromConnectionStringWithUrlEncodedPasswordAndUsername() {
        var redisConnectionString = "redis://some-user:some-access-key%3D@some-host:6379?key=value";
        var redisProperties = RedisConnectionConfig.redisConnectionPropertiesFrom(redisConnectionString);

        assertThat(redisProperties.host()).isEqualTo("some-host");
        assertThat(redisProperties.port()).isEqualTo(6379);
        assertThat(redisProperties.password()).isEqualTo("some-access-key=".toCharArray());
        assertThat(redisProperties.username()).isEqualTo("some-user");
    }


}
