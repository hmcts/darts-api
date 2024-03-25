package uk.gov.hmcts.darts.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import uk.gov.hmcts.darts.audio.model.AudioPreview;

@Configuration
@EnableCaching
@Slf4j
public class AudioPreviewCacheConfig {

    @Bean
    public RedisTemplate<String, AudioPreview> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String,AudioPreview> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setDefaultSerializer(new JdkSerializationRedisSerializer());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JdkSerializationRedisSerializer());
        return template;
    }

}
