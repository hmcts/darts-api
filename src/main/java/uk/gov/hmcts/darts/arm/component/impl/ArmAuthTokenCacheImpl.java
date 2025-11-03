package uk.gov.hmcts.darts.arm.component.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenResponse;
import uk.gov.hmcts.darts.arm.client.model.AvailableEntitlementProfile;
import uk.gov.hmcts.darts.arm.client.model.rpo.EmptyRpoRequest;
import uk.gov.hmcts.darts.arm.component.ArmAuthTokenCache;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;
import uk.gov.hmcts.darts.arm.service.ArmClientService;

import java.time.Duration;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.hmcts.darts.common.util.RedisConstants.ARM_TOKEN_CACHE;
import static uk.gov.hmcts.darts.common.util.RedisConstants.ARM_TOKEN_CACHE_KEY;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArmAuthTokenCacheImpl implements ArmAuthTokenCache {

    private static final String LOCK_KEY = "lock:arm-token";
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    private final CacheManager cacheManager;
    private final StringRedisTemplate redis;
    private final ArmClientService armClientService;
    private final ArmApiConfigurationProperties armApiConfigurationProperties;

    /**
     * Returns a cached token when present; otherwise refreshes it under a short Redis lock.
     */
    @Override
    public String getToken(ArmTokenRequest armTokenRequest) {
        Cache cache = cacheManager.getCache(ARM_TOKEN_CACHE);
        if (cache == null) {
            return fetchFreshToken(armTokenRequest);
        }

        String cached = cache.get(ARM_TOKEN_CACHE_KEY, String.class);
        if (StringUtils.hasText(cached)) {
            return cached;
        }

        boolean locked = tryAcquireLock();
        try {
            // Re-check after obtaining the lock to avoid duplicate refreshes across pods
            cached = cache.get(ARM_TOKEN_CACHE_KEY, String.class);
            if (StringUtils.hasText(cached)) {
                return cached;
            }

            String fresh = fetchFreshToken(armTokenRequest);
            if (StringUtils.hasText(fresh)) {
                cache.put(ARM_TOKEN_CACHE_KEY, fresh);
            }
            return fresh;
        } finally {
            if (locked) {
                releaseLock();
            }
        }
    }

    /**
     * Allows callers to dump a bad token (e.g. after 401) so next call will refresh.
     */
    @Override
    public void evictToken() {
        Cache cache = cacheManager.getCache(ARM_TOKEN_CACHE);
        if (cache != null) {
            cache.evict(ARM_TOKEN_CACHE_KEY);
        }
    }

    @SuppressWarnings("PMD.AvoidDeeplyNestedIfStmts")
    private String fetchFreshToken(ArmTokenRequest armTokenRequest) {
        String accessToken = null;
        try {
            ArmTokenResponse armTokenResponse = armClientService.getToken(armTokenRequest);
            if (isNotEmpty(armTokenResponse.getAccessToken())) {
                String bearerToken = String.format("Bearer %s", armTokenResponse.getAccessToken());
                log.debug("Fetched ARM Bearer Token from token: {}", bearerToken);
                EmptyRpoRequest emptyRpoRequest = EmptyRpoRequest.builder().build();

                AvailableEntitlementProfile availableEntitlementProfile = getAvailableEntitlementProfile(bearerToken, emptyRpoRequest, armTokenRequest);
                if (!availableEntitlementProfile.isError()) {
                    Optional<String> profileId = availableEntitlementProfile.getProfiles().stream()
                        .filter(p -> armApiConfigurationProperties.getArmServiceProfile().equalsIgnoreCase(p.getProfileName()))
                        .map(AvailableEntitlementProfile.Profiles::getProfileId)
                        .findAny();
                    if (profileId.isPresent()) {
                        log.debug("Found DARTS ARM Service Profile Id: {}", profileId.get());
                        ArmTokenResponse tokenResponse = armClientService.selectEntitlementProfile(bearerToken, profileId.get(), emptyRpoRequest);
                        accessToken = tokenResponse.getAccessToken();
                    }
                }
            }

            log.debug("Fetched ARM Bearer Token : {}", accessToken);
            return String.format("Bearer %s", accessToken);
        } catch (RestClientException ex) {
            log.error("Unable to fetch ARM auth token {}", ex.getMessage());
            throw ex;
        }
    }

    private AvailableEntitlementProfile getAvailableEntitlementProfile(String bearerToken, EmptyRpoRequest emptyRpoRequest, ArmTokenRequest armTokenRequest) {
        try {
            return armClientService.availableEntitlementProfiles(bearerToken, emptyRpoRequest);
        } catch (HttpClientErrorException ex) {
            int sc = ex.getStatusCode().value();
            if (sc == HttpStatus.UNAUTHORIZED.value() || sc == HttpStatus.FORBIDDEN.value()) {
                evictToken();
                ArmTokenResponse tokenResponse = armClientService.getToken(armTokenRequest);
                if (isNotEmpty(tokenResponse.getAccessToken())) {
                    String accessToken = String.format("Bearer %s", tokenResponse.getAccessToken());
                    return armClientService.availableEntitlementProfiles(accessToken, emptyRpoRequest);
                }
                throw ex;
            }
            throw ex;
        }
    }

    private boolean tryAcquireLock() {
        try {
            Boolean ok = redis.opsForValue().setIfAbsent(LOCK_KEY, "1", LOCK_TTL);
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            // If Redis is unhappy, proceed without the lock (worst case: extra token call)
            log.warn("Unable to acquire lock {}", e.getMessage());
            return false;
        }
    }

    private void releaseLock() {
        try {
            redis.delete(LOCK_KEY);
        } catch (Exception ignored) {
            log.warn("Release lock ignoring lock {}", ignored.getMessage());
        }
    }
}
