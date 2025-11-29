package uk.gov.hmcts.darts.arm.component.impl;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenResponse;
import uk.gov.hmcts.darts.arm.client.model.AvailableEntitlementProfile;
import uk.gov.hmcts.darts.arm.client.model.rpo.EmptyRpoRequest;
import uk.gov.hmcts.darts.arm.component.ArmAuthTokenCache;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;
import uk.gov.hmcts.darts.arm.service.ArmClientService;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.time.Duration;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.hmcts.darts.common.util.ArmRedisConstants.ARM_TOKEN_CACHE_KEY;
import static uk.gov.hmcts.darts.common.util.ArmRedisConstants.ARM_TOKEN_CACHE_NAME;

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
     * Allows callers to dump a bad token so next call will refresh.
     */
    @Override
    public void evictToken() {
        Cache cache = cacheManager.getCache(ARM_TOKEN_CACHE_NAME);
        if (cache != null) {
            cache.evict(ARM_TOKEN_CACHE_KEY);
        }
    }

    /**
     * Returns a cached token when present; otherwise refreshes it under a short Redis lock.
     */
    @Override
    public String getToken(ArmTokenRequest armTokenRequest) {
        Cache cache = cacheManager.getCache(ARM_TOKEN_CACHE_NAME);
        if (cache == null) {
            return fetchFreshBearerToken(armTokenRequest); // fail-open: no cache manager
        }

        String cached = cache.get(ARM_TOKEN_CACHE_KEY, String.class);
        if (StringUtils.hasText(cached)) {
            return cached;
        }

        return lockAndRetryGetToken(armTokenRequest, cache);
    }

    private String lockAndRetryGetToken(ArmTokenRequest armTokenRequest, Cache cache) {
        String cached;
        boolean locked = tryAcquireLock();
        try {
            // Re-check after obtaining the lock to avoid duplicate refreshes across pods
            cached = cache.get(ARM_TOKEN_CACHE_KEY, String.class);
            if (StringUtils.hasText(cached)) {
                return cached;
            }

            String freshBearer = fetchFreshBearerToken(armTokenRequest);
            // Only cache if truly valid (non-blank and not "Bearer null")
            if (StringUtils.hasText(freshBearer) && !"Bearer null".equals(freshBearer)) {
                cache.put(ARM_TOKEN_CACHE_KEY, freshBearer);
            }
            return freshBearer;
        } finally {
            if (locked) {
                releaseLock();
            }
        }
    }

    @SuppressWarnings("PMD.CyclomaticComplexity")
    private String fetchFreshBearerToken(ArmTokenRequest armTokenRequest) {
        try {
            ArmTokenResponse initial = armClientService.getToken(armTokenRequest);
            String firstAccessToken = initial != null ? initial.getAccessToken() : null;
            if (!isNotEmpty(firstAccessToken)) {
                throw new DartsApiException(CommonApiError.INTERNAL_SERVER_ERROR,
                                            "ARM token returned empty access token");
            }

            String firstBearer = String.format("Bearer %s", firstAccessToken);

            EmptyRpoRequest emptyRpoRequest = EmptyRpoRequest.builder().build();
            AvailableEntitlementProfile profiles = getAvailableEntitlementProfile(firstBearer, emptyRpoRequest, armTokenRequest);

            if (profiles == null || profiles.isError() || profiles.getProfiles() == null) {
                throw new DartsApiException(CommonApiError.INTERNAL_SERVER_ERROR,
                                            "ARM entitlement profiles unavailable or error returned");
            }

            Optional<String> profileId = profiles.getProfiles().stream()
                .filter(p -> armApiConfigurationProperties.getArmServiceProfile().equalsIgnoreCase(p.getProfileName()))
                .map(AvailableEntitlementProfile.Profiles::getProfileId)
                .findFirst();

            if (profileId.isEmpty()) {
                throw new DartsApiException(CommonApiError.INTERNAL_SERVER_ERROR,
                                            "ARM service profile not found: " + armApiConfigurationProperties.getArmServiceProfile());
            }

            ArmTokenResponse selected = armClientService.selectEntitlementProfile(firstBearer, profileId.get(), emptyRpoRequest);
            String finalAccessToken = selected != null ? selected.getAccessToken() : null;

            if (!isNotEmpty(finalAccessToken)) {
                throw new DartsApiException(CommonApiError.INTERNAL_SERVER_ERROR,
                                            "ARM selectEntitlementProfile returned empty access token");
            }
            
            return String.format("Bearer %s", finalAccessToken);

        } catch (FeignException ex) {
            log.error("Unable to fetch ARM auth token: {}", ex.getMessage());
            throw ex;
        }
    }

    private AvailableEntitlementProfile getAvailableEntitlementProfile(
        String bearerToken, EmptyRpoRequest emptyRpoRequest, ArmTokenRequest armTokenRequest) {

        try {
            return armClientService.availableEntitlementProfiles(bearerToken, emptyRpoRequest);
        } catch (FeignException ex) {
            int status = ex.status();
            if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
                // Evict and retry once with a brand new bearer token
                evictToken();
                ArmTokenResponse tokenResponse = armClientService.getToken(armTokenRequest);
                String access = tokenResponse != null ? tokenResponse.getAccessToken() : null;

                if (isEmpty(access)) {
                    log.warn("ARM token returned empty access token during profiles retry");
                    throw ex;
                }

                String freshBearer = "Bearer " + access;
                return armClientService.availableEntitlementProfiles(freshBearer, emptyRpoRequest);
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
