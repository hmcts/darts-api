package uk.gov.hmcts.darts.arm.component.impl;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenResponse;
import uk.gov.hmcts.darts.arm.client.model.AvailableEntitlementProfile;
import uk.gov.hmcts.darts.arm.client.model.rpo.EmptyRpoRequest;
import uk.gov.hmcts.darts.arm.component.ArmAuthTokenCache;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;
import uk.gov.hmcts.darts.arm.service.ArmClientService;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.hmcts.darts.common.util.ArmRedisConstants.ARM_TOKEN_CACHE_NAME;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArmAuthTokenCacheImpl implements ArmAuthTokenCache {

    private final ArmClientService armClientService;
    private final ArmApiConfigurationProperties armApiConfigurationProperties;

    /**
     * Allows callers to dump a bad token so next call will refresh.
     */
    @Override
    @CacheEvict(value = ARM_TOKEN_CACHE_NAME, allEntries = true, cacheManager = "armRedisCacheManager")
    public void evictToken() {
        log.warn("Evicting ARM token from cache");
    }

    @Override
    @Cacheable(value = ARM_TOKEN_CACHE_NAME, cacheManager = "armRedisCacheManager", sync = true)
    public String getToken(ArmTokenRequest armTokenRequest) {
        return fetchFreshBearerToken(armTokenRequest);
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

}
