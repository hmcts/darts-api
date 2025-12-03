package uk.gov.hmcts.darts.arm.component;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenResponse;
import uk.gov.hmcts.darts.arm.client.model.AvailableEntitlementProfile;
import uk.gov.hmcts.darts.arm.client.model.rpo.EmptyRpoRequest;
import uk.gov.hmcts.darts.arm.client.version.fivetwo.ArmApiBaseClient;
import uk.gov.hmcts.darts.arm.client.version.fivetwo.ArmAuthClient;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.util.ArmRedisConstants.ARM_TOKEN_CACHE_NAME;

@TestPropertySource(properties = {"darts.storage.arm-api.enable-arm-v5-2-upgrade=true"})
@Isolated
@Profile("in-memory-caching")
class ArmAuthTokenCacheIntTest extends IntegrationBase {

    @Autowired
    private ArmAuthTokenCache armAuthTokenCache;

    @Autowired
    private ArmApiConfigurationProperties armApiConfigurationProperties;

    @MockitoBean
    private ArmAuthClient armAuthClient;
    @MockitoBean
    private ArmApiBaseClient armApiBaseClient;

    @TestConfiguration
    static class TestCacheConfig {
        @Bean(name = "armRedisCacheManager")
        public CacheManager armRedisCacheManager() {
            return new ConcurrentMapCacheManager(ARM_TOKEN_CACHE_NAME);
        }
    }

    @BeforeEach
    void setUp() {
        ArmTokenRequest armTokenRequest = ArmTokenRequest.builder()
            .username(armApiConfigurationProperties.getArmUsername())
            .password(armApiConfigurationProperties.getArmPassword())
            .build();
        ArmTokenResponse armTokenResponse = getArmTokenResponse();
        String bearerToken = String.format("Bearer %s", armTokenResponse.getAccessToken());
        when(armAuthClient.getToken(armTokenRequest))
            .thenReturn(armTokenResponse);
        EmptyRpoRequest emptyRpoRequest = EmptyRpoRequest.builder().build();
        when(armApiBaseClient.availableEntitlementProfiles(bearerToken, emptyRpoRequest))
            .thenReturn(getAvailableEntitlementProfile());
        when(armApiBaseClient.selectEntitlementProfile(bearerToken, "some-profile-id", emptyRpoRequest))
            .thenReturn(armTokenResponse);
    }

    private AvailableEntitlementProfile getAvailableEntitlementProfile() {
        List<AvailableEntitlementProfile.Profiles> profiles = List.of(AvailableEntitlementProfile.Profiles.builder()
                                                                          .profileName("some-profile-name")
                                                                          .profileId("some-profile-id")
                                                                          .build());

        return AvailableEntitlementProfile.builder()
            .profiles(profiles)
            .isError(false)
            .build();
    }

    @Test
    @Order(1)
    void getToken_ReturnsNewToken() {
        EmptyRpoRequest emptyRpoRequest = EmptyRpoRequest.builder().build();
        ArmTokenRequest armTokenRequest = ArmTokenRequest.builder()
            .username(armApiConfigurationProperties.getArmUsername())
            .password(armApiConfigurationProperties.getArmPassword())
            .build();

        String token = armAuthTokenCache.getToken(armTokenRequest);

        assertEquals("Bearer some-token", token);
        verify(armAuthClient).getToken(armTokenRequest);
        verify(armApiBaseClient).availableEntitlementProfiles("Bearer some-token", emptyRpoRequest);
        verify(armApiBaseClient).selectEntitlementProfile("Bearer some-token", "some-profile-id", emptyRpoRequest);
        verifyNoMoreInteractions(armAuthClient);
        verifyNoMoreInteractions(armApiBaseClient);
    }

    @Test
    @Order(2)
    void getToken_ReturnsCachedToken() {
        ArmTokenRequest armTokenRequest = ArmTokenRequest.builder()
            .username(armApiConfigurationProperties.getArmUsername())
            .password(armApiConfigurationProperties.getArmPassword())
            .build();

        String cachedToken = armAuthTokenCache.getToken(armTokenRequest);

        assertEquals("Bearer some-token", cachedToken);
        verifyNoMoreInteractions(armAuthClient);
        verifyNoMoreInteractions(armApiBaseClient);
    }

    private ArmTokenResponse getArmTokenResponse() {
        return ArmTokenResponse.builder()
            .accessToken("some-token")
            .tokenType("Bearer")
            .expiresIn("3600")
            .build();
    }

}
