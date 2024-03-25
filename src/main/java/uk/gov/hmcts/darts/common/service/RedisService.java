package uk.gov.hmcts.darts.common.service;

import java.util.concurrent.TimeUnit;

public interface RedisService<T> {

    void writeToRedis(String folder, String key, T data);

    T readFromRedis(String folder, String key);

    void deleteFromRedis(String folder, String key);

    void setTtl(String folder, String key, long timeout, TimeUnit timeUnit);

}