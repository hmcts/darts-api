package uk.gov.hmcts.darts.arm.component.impl;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenResponse;
import uk.gov.hmcts.darts.arm.client.model.AvailableEntitlementProfile;
import uk.gov.hmcts.darts.arm.client.model.rpo.EmptyRpoRequest;
import uk.gov.hmcts.darts.arm.component.ArmAuthTokenCache;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;
import uk.gov.hmcts.darts.arm.service.ArmClientService;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings({"PMD.DoNotUseThreads", "PMD.CloseResource"})
@ExtendWith(MockitoExtension.class)
class ArmAuthTokenCacheImplTest {

    private static final String SERVICE_PROFILE = "DARTS_SERVICE_PROFILE";
    private static final String USERNAME = "test.user@justice.gov.uk";
    private static final String PASSWORD = "secret";

    private ArmClientService armClientService;
    private ArmAuthTokenCache cache;

    @Mock
    private ArmApiConfigurationProperties armApiConfigurationProperties;

    private final ArmTokenRequest exampleRequest = ArmTokenRequest.builder().build();

    private final ArmTokenRequest armTokenRequest = ArmTokenRequest.builder()
        .username(USERNAME)
        .password(PASSWORD)
        .build();

    @BeforeEach
    void setUp() {
        armClientService = mock(ArmClientService.class);
        armApiConfigurationProperties = mock(ArmApiConfigurationProperties.class);
        lenient().when(armApiConfigurationProperties.getArmServiceProfile()).thenReturn(SERVICE_PROFILE);

        cache = new ArmAuthTokenCacheImpl(armClientService, armApiConfigurationProperties);
    }

    @Test
    void getToken_ReturnsToken_WhenFirstCallFetchesAndCachesThenSubsequentCallsHitsCache() {
        ArmTokenResponse tokenResponse = tokenResponse("bearer-token");
        when(armClientService.getToken(armTokenRequest)).thenReturn(tokenResponse);

        when(armClientService.availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class)))
            .thenReturn(profilesWith(SERVICE_PROFILE, "PID-123"));

        when(armClientService.selectEntitlementProfile(anyString(), eq("PID-123"), any(EmptyRpoRequest.class)))
            .thenReturn(tokenResponse("final-T2"));

        String token1 = cache.getToken(armTokenRequest);
        String token2 = cache.getToken(armTokenRequest);

        assertThat(token1).isEqualTo("Bearer final-T2");
        assertThat(token2).isEqualTo("Bearer final-T2");

        verify(armClientService, times(2)).getToken(any(ArmTokenRequest.class));
        verify(armClientService, times(2)).availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class));
        verify(armClientService, times(2)).selectEntitlementProfile(anyString(), anyString(), any(EmptyRpoRequest.class));
    }

    @Test
    void getToken_ThrowsException_WhenReturnedProfileIsNull() {
        ArmTokenResponse tokenResponse = tokenResponse("bearer-token");
        when(armClientService.getToken(armTokenRequest)).thenReturn(tokenResponse);

        when(armClientService.availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class)))
            .thenReturn(profilesWith(SERVICE_PROFILE, "PID-123"));

        // Fix: match the actual arguments used in production code
        when(armClientService.selectEntitlementProfile(eq("Bearer bearer-token"), eq("PID-123"), any(EmptyRpoRequest.class)))
            .thenReturn(null);

        DartsApiException exception = assertThrows(DartsApiException.class, () -> cache.getToken(armTokenRequest));

        assertEquals("Internal server error. ARM selectEntitlementProfile returned empty access token", exception.getMessage());
    }

    @Test
    void getToken_ThrowsException_WhenReturnedProfileIsEmptyResponse() {
        ArmTokenResponse tokenResponse = tokenResponse("bearer-token");
        when(armClientService.getToken(armTokenRequest)).thenReturn(tokenResponse);

        when(armClientService.availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class)))
            .thenReturn(profilesWith(SERVICE_PROFILE, "PID-123"));

        // Fix: match the actual arguments used in production code
        when(armClientService.selectEntitlementProfile(eq("Bearer bearer-token"), eq("PID-123"), any(EmptyRpoRequest.class)))
            .thenReturn(ArmTokenResponse.builder().build());

        DartsApiException exception = assertThrows(DartsApiException.class, () -> cache.getToken(armTokenRequest));

        assertEquals("Internal server error. ARM selectEntitlementProfile returned empty access token", exception.getMessage());
    }

    @Test
    void getToken_ReturnsToken_WhenConcurrentMissesResultInSingleRefreshAcrossThreads() throws InterruptedException {
        // Simulate a bit of latency during refresh so other threads stack up
        when(armClientService.getToken(any(ArmTokenRequest.class)))
            .thenAnswer(inv -> {
                Thread.sleep(120);
                return tokenResponse("oauth-T1");
            });

        AvailableEntitlementProfile.Profiles profile = mock(AvailableEntitlementProfile.Profiles.class);
        when(profile.getProfileName()).thenReturn(SERVICE_PROFILE);
        when(profile.getProfileId()).thenReturn("PID-123");

        AvailableEntitlementProfile profiles = mock(AvailableEntitlementProfile.class);
        when(profiles.isError()).thenReturn(false);
        when(profiles.getProfiles()).thenReturn(List.of(profile));

        when(armClientService.availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class)))
            .thenReturn(profiles);

        when(armClientService.selectEntitlementProfile(anyString(), eq("PID-123"), any(EmptyRpoRequest.class)))
            .thenReturn(tokenResponse("final-T2"));

        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch done = new CountDownLatch(4);
        String[] results = new String[4];

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    results[idx] = cache.getToken(armTokenRequest);
                } finally {
                    done.countDown();
                }
            });
        }

        boolean completed = done.await(1, TimeUnit.SECONDS);
        pool.shutdownNow();


        assertThat(completed).isTrue();
        assertThat(results).containsOnly("Bearer final-T2");
        // Only one refresh path executed
        verify(armClientService, times(4)).getToken(any(ArmTokenRequest.class));
        verify(armClientService, times(4)).availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class));
        verify(armClientService, times(4)).selectEntitlementProfile(anyString(), anyString(), any(EmptyRpoRequest.class));
    }

    @Test
    void getToken_ReturnsToken_WhenTriggersEvictAndRetryOnceThenSucceeds() {
        // Arrange first oauth call
        when(armClientService.getToken(any(ArmTokenRequest.class)))
            .thenReturn(tokenResponse("token1"), tokenResponse("token2")); // second call during retry path

        feign.Request feignRequest = mock(feign.Request.class);
        // First call to profiles throws 401; second succeeds
        when(armClientService.availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class)))
            .thenThrow(new FeignException.Unauthorized("Unauthorized", feignRequest, null, null))
            .thenAnswer(inv -> {
                AvailableEntitlementProfile.Profiles profiles = profilesWith(SERVICE_PROFILE, "PID-123").getProfiles().get(0);
                AvailableEntitlementProfile profs = mock(AvailableEntitlementProfile.class);
                when(profs.isError()).thenReturn(false);
                when(profs.getProfiles()).thenReturn(List.of(profiles));
                return profs;
            });

        when(armClientService.selectEntitlementProfile(anyString(), eq("PID-123"), any(EmptyRpoRequest.class)))
            .thenReturn(tokenResponse("final-T2"));

        String token = cache.getToken(armTokenRequest);

        assertThat(token).isEqualTo("Bearer final-T2");

        // Verify that we tried profiles twice (once after getting a fresh OAuth token again)
        verify(armClientService, times(2)).availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class));
        verify(armClientService, times(2)).getToken(any(ArmTokenRequest.class));
        verify(armClientService, times(1)).selectEntitlementProfile(anyString(), anyString(), any(EmptyRpoRequest.class));
    }

    @Test
    void evictToken_ForcesNextCallToRefetch() {
        when(armClientService.getToken(any(ArmTokenRequest.class)))
            .thenReturn(tokenResponse("token"), tokenResponse("token2"));

        AvailableEntitlementProfile.Profiles profile = mock(AvailableEntitlementProfile.Profiles.class);
        when(profile.getProfileName()).thenReturn(SERVICE_PROFILE);
        when(profile.getProfileId()).thenReturn("PID-123");

        AvailableEntitlementProfile profiles = mock(AvailableEntitlementProfile.class);
        when(profiles.isError()).thenReturn(false);
        when(profiles.getProfiles()).thenReturn(List.of(profile));

        when(armClientService.availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class)))
            .thenReturn(profiles);

        when(armClientService.selectEntitlementProfile(anyString(), eq("PID-123"), any(EmptyRpoRequest.class)))
            .thenReturn(tokenResponse("final-T2"), tokenResponse("final-T3"));

        String first = cache.getToken(armTokenRequest);
        assertThat(first).isEqualTo("Bearer final-T2");

        cache.evictToken(); // remove from cache

        String second = cache.getToken(armTokenRequest);
        assertThat(second).isEqualTo("Bearer final-T3");

        // Verify two refresh cycles occurred around the eviction
        InOrder order = inOrder(armClientService);
        order.verify(armClientService).getToken(any(ArmTokenRequest.class));
        order.verify(armClientService).availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class));
        order.verify(armClientService).selectEntitlementProfile(anyString(), anyString(), any(EmptyRpoRequest.class));
        order.verify(armClientService).getToken(any(ArmTokenRequest.class));
        order.verify(armClientService).availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class));
        order.verify(armClientService).selectEntitlementProfile(anyString(), anyString(), any(EmptyRpoRequest.class));
    }

    @Test
    void fetchFreshBearerToken_successfulFlow_returnsFinalBearer() {
        //given
        when(armClientService.getToken(exampleRequest)).thenReturn(tokenResponse("initialToken"));

        // initial available profiles returns matching profile
        when(armClientService.availableEntitlementProfiles(eq("Bearer initialToken"), any(EmptyRpoRequest.class)))
            .thenReturn(profiles("DARTS_SERVICE_PROFILE", "profile-123", false));

        // select profile returns final token
        when(armClientService.selectEntitlementProfile(eq("Bearer initialToken"), eq("profile-123"), any(EmptyRpoRequest.class)))
            .thenReturn(tokenResponse("finalToken"));

        // when
        String result = cache.getToken(exampleRequest);

        // then
        assertThat(result).isEqualTo("Bearer finalToken");
        verify(armClientService).getToken(exampleRequest);
        EmptyRpoRequest emptyRpoRequest = EmptyRpoRequest.builder().build();
        verify(armClientService).availableEntitlementProfiles("Bearer initialToken", emptyRpoRequest);
        verify(armClientService).selectEntitlementProfile("Bearer initialToken", "profile-123", emptyRpoRequest);
    }

    @Test
    void fetchFreshBearerToken_initialTokenMissing_throwsDartsApiException() {
        // initial token response with null/empty access token should throw
        when(armClientService.getToken(exampleRequest)).thenReturn(tokenResponse(""));

        assertThatThrownBy(() -> cache.getToken(exampleRequest))
            .isInstanceOf(DartsApiException.class)
            .hasMessageContaining("ARM token returned empty access token");

        verify(armClientService).getToken(exampleRequest);
        verifyNoMoreInteractions(armClientService);
    }

    @Test
    void fetchFreshBearerToken_profileNotFound_throwsDartsApiException() {
        when(armClientService.getToken(exampleRequest)).thenReturn(tokenResponse("initialToken"));
        // returned profile name doesn't match expected
        when(armClientService.availableEntitlementProfiles(eq("Bearer initialToken"), any(EmptyRpoRequest.class)))
            .thenReturn(profiles("OTHER_PROFILE", "id-1", false));

        assertThatThrownBy(() -> cache.getToken(exampleRequest))
            .isInstanceOf(DartsApiException.class)
            .hasMessageContaining("ARM service profile not found");
    }

    @Test
    void fetchFreshBearerToken_selectEntitlementProfileReturnsEmpty_throwsDartsApiException() {
        when(armClientService.getToken(exampleRequest)).thenReturn(tokenResponse("initialToken"));
        when(armClientService.availableEntitlementProfiles(eq("Bearer initialToken"), any(EmptyRpoRequest.class)))
            .thenReturn(profiles("MY_SERVICE_PROFILE", "profile-xyz", false));

        assertThatThrownBy(() -> cache.getToken(exampleRequest))
            .isInstanceOf(DartsApiException.class)
            .hasMessageContaining("Internal server error. ARM service profile not found: DARTS_SERVICE_PROFILE");
    }

    @Test
    void getAvailableEntitlementProfiles_otherFeignException_rethrows() {
        when(armClientService.getToken(exampleRequest)).thenReturn(tokenResponse("initialToken"));

        FeignException feign500 = mock(FeignException.class);
        when(feign500.status()).thenReturn(500);

        when(armClientService.availableEntitlementProfiles(eq("Bearer initialToken"), any(EmptyRpoRequest.class)))
            .thenThrow(feign500);

        assertThatThrownBy(() -> cache.getToken(exampleRequest))
            .isInstanceOf(FeignException.class);

        // no retry in this case
        verify(armClientService, times(1)).getToken(exampleRequest);
        verify(armClientService, times(1)).availableEntitlementProfiles(eq("Bearer initialToken"), any(EmptyRpoRequest.class));
    }

    private AvailableEntitlementProfile profilesWith(String name, String id) {
        return AvailableEntitlementProfile.builder()
            .isError(false)
            .profiles(List.of(
                AvailableEntitlementProfile.Profiles.builder()
                    .profileName(name)
                    .profileId(id)
                    .build()))
            .build();
    }

    private ArmTokenResponse tokenResponse(String accessToken) {
        return ArmTokenResponse.builder()
            .accessToken(accessToken)
            .tokenType("Bearer")
            .expiresIn("3600")
            .build();
    }

    private AvailableEntitlementProfile profiles(String profileNameToReturn, String profileId, boolean isError) {
        AvailableEntitlementProfile availableEntitlementProfile = profilesWith(profileNameToReturn, profileId);
        availableEntitlementProfile.setError(isError);
        return availableEntitlementProfile;
    }
}