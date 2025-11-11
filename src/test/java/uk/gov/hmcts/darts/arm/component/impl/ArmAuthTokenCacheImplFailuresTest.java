package uk.gov.hmcts.darts.arm.component.impl;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenResponse;
import uk.gov.hmcts.darts.arm.client.model.AvailableEntitlementProfile;
import uk.gov.hmcts.darts.arm.client.model.rpo.EmptyRpoRequest;
import uk.gov.hmcts.darts.arm.component.ArmAuthTokenCache;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;
import uk.gov.hmcts.darts.arm.service.ArmClientService;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.util.ArmRedisConstants.ARM_TOKEN_CACHE_NAME;

@SuppressWarnings({"unchecked", "PMD.DoNotUseThreads", "PMD.CloseResource"})
@ExtendWith(MockitoExtension.class)
class ArmAuthTokenCacheImplFailuresTest {

    private static final String SERVICE_PROFILE = "DARTS_SERVICE_PROFILE";
    private static final String PROFILE_ID = "PID-123";

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private ArmClientService armClientService;
    @Mock
    private ArmApiConfigurationProperties props;

    private ArmAuthTokenCache cache; // SUT

    private ArmTokenRequest tokenReq;

    @BeforeEach
    void setUp() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(ARM_TOKEN_CACHE_NAME);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(props.getArmServiceProfile()).thenReturn(SERVICE_PROFILE);

        cache = new ArmAuthTokenCacheImpl(cacheManager, redisTemplate, armClientService, props);

        tokenReq = ArmTokenRequest.builder()
            .username("service.user@justice.gov.uk")
            .password("secret")
            .build();
    }

    @Test
    void getToken_throwsException_whenOAuthTokenIsEmpty() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(armClientService.getToken(any(ArmTokenRequest.class)))
            .thenReturn(armToken(null)); // empty access token

        assertThatThrownBy(() -> cache.getToken(tokenReq))
            .isInstanceOf(DartsApiException.class)
            .hasMessageContaining("ARM token returned empty access token");
    }

    @Test
    void getToken_throwsException_whenProfilesResponseIsErrorOrEmpty() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(armClientService.getToken(any(ArmTokenRequest.class)))
            .thenReturn(armToken("oauth-T1"));

        // error=true, or empty list both should fail
        when(armClientService.availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class)))
            .thenReturn(AvailableEntitlementProfile.builder().isError(true).profiles(null).build());

        assertThatThrownBy(() -> cache.getToken(tokenReq))
            .isInstanceOf(DartsApiException.class)
            .hasMessageContaining("entitlement profiles unavailable");
    }

    @Test
    void getToken_throwsException_whenServiceProfileNotFound() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(armClientService.getToken(any(ArmTokenRequest.class)))
            .thenReturn(armToken("oauth-T1"));

        // returns a profile with a different name
        when(armClientService.availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class)))
            .thenReturn(profilesWith("SOME_OTHER_PROFILE", "X"));

        assertThatThrownBy(() -> cache.getToken(tokenReq))
            .isInstanceOf(DartsApiException.class)
            .hasMessageContaining("service profile not found");
    }

    @Test
    void getToken_throwsException_whenSelectEntitlementReturnsEmptyAccessToken() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(armClientService.getToken(any(ArmTokenRequest.class)))
            .thenReturn(armToken("oauth-T1"));

        when(armClientService.availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class)))
            .thenReturn(profilesWith(SERVICE_PROFILE, PROFILE_ID));

        when(armClientService.selectEntitlementProfile(anyString(), eq(PROFILE_ID), any(EmptyRpoRequest.class)))
            .thenReturn(armToken(null)); // empty final token

        assertThatThrownBy(() -> cache.getToken(tokenReq))
            .isInstanceOf(DartsApiException.class)
            .hasMessageContaining("ARM selectEntitlementProfile returned empty access token");
    }

    @Test
    void getToken_throwsException_fromOAuthCall() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(armClientService.getToken(any(ArmTokenRequest.class)))
            .thenThrow(new RestClientException("arm down"));

        assertThatThrownBy(() -> cache.getToken(tokenReq))
            .isInstanceOf(RestClientException.class)
            .hasMessageContaining("arm down");
    }

    @Test
    void getToken_returnsBearerToken_whenEvictsAndRetriesUnauthorised() {
        // first OAuth
        when(armClientService.getToken(any(ArmTokenRequest.class)))
            .thenReturn(armToken("oauth-A"))
            .thenReturn(armToken("oauth-B")); // retried OAuth after 401

        // first profiles call -> 401
        feign.Request feignRequest = mock(feign.Request.class);
        when(armClientService.availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class)))
            .thenThrow(new FeignException.Unauthorized("Unauthorized", feignRequest, null, null))
            .thenReturn(profilesWith(SERVICE_PROFILE, PROFILE_ID)); // retry returns OK

        when(armClientService.selectEntitlementProfile(anyString(), eq(PROFILE_ID), any(EmptyRpoRequest.class)))
            .thenReturn(armToken("final-T"));

        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(true);

        String bearer = cache.getToken(tokenReq);
        assertThat(bearer).isEqualTo("Bearer final-T");

        // Validate the retry happened (2x token + 2x profiles, 1x select)
        verify(armClientService, times(2)).getToken(any(ArmTokenRequest.class));
        verify(armClientService, times(2)).availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class));
        verify(armClientService, times(1)).selectEntitlementProfile(anyString(), eq(PROFILE_ID), any(EmptyRpoRequest.class));
    }

    @Test
    void getToken_returnsBearerToken_whenEvictsAndRetriesForbidden() {
        when(armClientService.getToken(any(ArmTokenRequest.class)))
            .thenReturn(armToken("oauth-A"))
            .thenReturn(armToken("oauth-B"));

        feign.Request feignRequest = mock(feign.Request.class);
        when(armClientService.availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class)))
            .thenThrow(new FeignException.Forbidden("Forbidden", feignRequest, null, null))
            .thenReturn(profilesWith(SERVICE_PROFILE, PROFILE_ID));

        when(armClientService.selectEntitlementProfile(anyString(), eq(PROFILE_ID), any(EmptyRpoRequest.class)))
            .thenReturn(armToken("final-T"));

        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(true);

        String bearer = cache.getToken(tokenReq);
        assertThat(bearer).isEqualTo("Bearer final-T");

        verify(armClientService, times(2)).getToken(any(ArmTokenRequest.class));
        verify(armClientService, times(2)).availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class));
        verify(armClientService, times(1)).selectEntitlementProfile(anyString(), eq(PROFILE_ID), any(EmptyRpoRequest.class));
    }

    @Test
    void getToken_returnsBearerToken_withoutRetry() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(armClientService.getToken(any(ArmTokenRequest.class)))
            .thenReturn(armToken("oauth-T1"));

        when(armClientService.availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "bad request"));

        assertThatThrownBy(() -> cache.getToken(tokenReq))
            .isInstanceOf(HttpClientErrorException.class)
            .hasMessageContaining("bad request");

        // no second token call (no retry for non-401/403)
        verify(armClientService, times(1)).getToken(any(ArmTokenRequest.class));
    }

    @Test
    void getToken_returnsBearerToken_whenLockAcquireThrowsException() {
        // lock acquisition itself throws; code should fail-open and still fetch
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenThrow(new RuntimeException("redis hiccup"));

        stubHappyPath("oauth-X", "final-Z");

        String token = cache.getToken(tokenReq);
        assertThat(token).isEqualTo("Bearer final-Z");

        verify(armClientService, times(1)).getToken(any(ArmTokenRequest.class));
        verify(armClientService, times(1)).availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class));
        verify(armClientService, times(1)).selectEntitlementProfile(anyString(), eq(PROFILE_ID), any(EmptyRpoRequest.class));
    }

    @Test
    void getToken_returnsBearerToken_releaseLockExceptionIsSwallowed() {
        // Acquire ok, then simulate delete failure on release
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(true);
        doThrow(new RuntimeException("delete failed")).when(redisTemplate).delete(startsWith("lock:arm-token"));

        stubHappyPath("oauth-X", "final-Z");

        String token = cache.getToken(tokenReq);
        assertThat(token).isEqualTo("Bearer final-Z");
    }

    @Test
    void getToken_returnsBearerToken_withNullCacheManagerAndFallsBackToDirectFetchWithNoCacheUsed() {
        // Use a mock CacheManager that returns null for getCache
        var concurrentMapCacheManager = mock(ConcurrentMapCacheManager.class);
        when(concurrentMapCacheManager.getCache(anyString())).thenReturn(null);

        // fresh ArmAuthTokenCache with null-cache behaviour
        var armAuthTokenCache = new ArmAuthTokenCacheImpl(concurrentMapCacheManager, redisTemplate, armClientService, props);

        // normal fetch chain; no caching
        stubHappyPath("oauth-T1", "final-T2");

        String token = armAuthTokenCache.getToken(tokenReq);
        assertThat(token).isEqualTo("Bearer final-T2");

        // calling again should force another full refresh (no cache available)
        when(armClientService.getToken(any(ArmTokenRequest.class)))
            .thenReturn(armToken("oauth-T3"));
        when(armClientService.selectEntitlementProfile(anyString(), eq(PROFILE_ID), any(EmptyRpoRequest.class)))
            .thenReturn(armToken("final-T4"));

        String token2 = armAuthTokenCache.getToken(tokenReq);
        assertThat(token2).isEqualTo("Bearer final-T4");

        // prove no cache puts happened by verifying Cache never used
        verify(concurrentMapCacheManager, atLeastOnce()).getCache(ARM_TOKEN_CACHE_NAME);
    }

    @Test
    @Timeout(3)
    void getToken_holdsLockAcrossThreads_withConcurrentMissesAndOnlyOneRefresh() throws InterruptedException {
        // first contender acquires lock, the rest see it as held
        when(valueOps.setIfAbsent(startsWith("lock:arm-token"), anyString(), any(Duration.class)))
            .thenReturn(true).thenReturn(false).thenReturn(false).thenReturn(false);

        when(armClientService.getToken(any(ArmTokenRequest.class)))
            .thenAnswer(inv -> {
                Thread.sleep(120);
                return armToken("oauth-T1");
            });

        when(armClientService.availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class)))
            .thenReturn(profilesWith(SERVICE_PROFILE, PROFILE_ID));

        when(armClientService.selectEntitlementProfile(anyString(), eq(PROFILE_ID), any(EmptyRpoRequest.class)))
            .thenReturn(armToken("final-T2"));

        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch done = new CountDownLatch(4);
        String[] results = new String[4];

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    results[idx] = cache.getToken(tokenReq);
                } finally {
                    done.countDown();
                }
            });
        }
        assertThat(done.await(2500, TimeUnit.MILLISECONDS)).isTrue();
        pool.shutdownNow();

        assertThat(results).containsOnly("Bearer final-T2");
        verify(armClientService, times(4)).getToken(any(ArmTokenRequest.class));
        verify(armClientService, times(4)).availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class));
        verify(armClientService, times(4)).selectEntitlementProfile(anyString(), eq(PROFILE_ID), any(EmptyRpoRequest.class));
    }

    @Test
    void getToken_returnsBearerToken_whenCacheHitSkipsNetworkCalls() {
        // First call populates cache
        stubHappyPath("oauth-A", "final-B");
        String first = cache.getToken(tokenReq);
        assertThat(first).isEqualTo("Bearer final-B");

        // Reset ARM stubs to fail if called again (proves cache hit)
        reset(armClientService);
        String second = cache.getToken(tokenReq);
        assertThat(second).isEqualTo("Bearer final-B");

        verifyNoInteractions(armClientService);
    }

    @Test
    void getToken_returnsBearerToken_whenFirstTokenEvictedAndRemovesCachedEntryAndNextCallRefetches() {
        // Populate cache via first refresh
        stubHappyPath("oauth-A", "final-B");
        String first = cache.getToken(tokenReq);
        assertThat(first).isEqualTo("Bearer final-B");

        // Evict and assert second refresh chain happens
        cache.evictToken();

        // set up new values to prove refetch
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(armClientService.getToken(any(ArmTokenRequest.class))).thenReturn(armToken("oauth-C"));
        when(armClientService.availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class))).thenReturn(profilesWith(SERVICE_PROFILE, PROFILE_ID));
        when(armClientService.selectEntitlementProfile(anyString(), eq(PROFILE_ID), any(EmptyRpoRequest.class))).thenReturn(armToken("final-D"));

        String second = cache.getToken(tokenReq);
        assertThat(second).isEqualTo("Bearer final-D");

        InOrder inOrder = inOrder(armClientService);
        inOrder.verify(armClientService).getToken(any(ArmTokenRequest.class));
        inOrder.verify(armClientService).availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class));
        inOrder.verify(armClientService).selectEntitlementProfile(anyString(), eq(PROFILE_ID), any(EmptyRpoRequest.class));
    }

    private void stubHappyPath(String oauthToken, String finalToken) {
        when(armClientService.getToken(any(ArmTokenRequest.class)))
            .thenReturn(armToken(oauthToken));

        when(armClientService.availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class)))
            .thenReturn(profilesWith(SERVICE_PROFILE, PROFILE_ID));

        when(armClientService.selectEntitlementProfile(anyString(), eq(PROFILE_ID), any(EmptyRpoRequest.class)))
            .thenReturn(armToken(finalToken));
    }

    private ArmTokenResponse armToken(String accessToken) {
        return ArmTokenResponse.builder().accessToken(accessToken).build();
    }

    private AvailableEntitlementProfile profilesWith(String name, String id) {
        return AvailableEntitlementProfile.builder()
            .isError(false)
            .profiles(List.of(
                AvailableEntitlementProfile.Profiles.builder()
                    .profileName(name)
                    .profileId(id)
                    .build()
            ))
            .build();
    }
}

