package uk.gov.hmcts.darts.common.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.service.RedisService;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
@Slf4j
public class RedisServiceImpl<T> implements RedisService<T> {

    private final RedisTemplate<String, T> redisTemplate;

    @Override
    public void writeToRedis(String folder, String key, T data) {
        String fullKey = folder + ":" + key;
        redisTemplate.opsForValue().set(fullKey, data);
    }

    @Override
    public T readFromRedis(String folder, String key) {
        String fullKey = folder + ":" + key;
        return redisTemplate.opsForValue().get(fullKey);
    }

    @Override
    public void deleteFromRedis(String folder, String key) {
        String fullKey = folder + ":" + key;
        redisTemplate.delete(fullKey);
    }

    @Override
    public void setTtl(String folder, String key, long timeout, TimeUnit timeUnit) {
        String fullKey = folder + ":" + key;
        redisTemplate.expire(fullKey, timeout, timeUnit);
    }

}