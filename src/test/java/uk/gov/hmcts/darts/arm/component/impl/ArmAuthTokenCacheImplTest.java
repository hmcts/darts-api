package uk.gov.hmcts.darts.arm.component.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenResponse;
import uk.gov.hmcts.darts.arm.client.model.AvailableEntitlementProfile;
import uk.gov.hmcts.darts.arm.client.model.rpo.EmptyRpoRequest;
import uk.gov.hmcts.darts.arm.component.ArmAuthTokenCache;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;
import uk.gov.hmcts.darts.arm.service.ArmClientService;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.util.ArmRedisConstants.ARM_TOKEN_CACHE_NAME;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class ArmAuthTokenCacheImplTest {

    private static final String SERVICE_PROFILE = "DARTS_SERVICE_PROFILE";
    private static final String USERNAME = "test.user@justice.gov.uk";
    private static final String PASSWORD = "secret";

    private ConcurrentMapCacheManager cacheManager;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private ArmClientService armClientService;
    private ArmApiConfigurationProperties armApiConfigurationProperties;
    private ArmAuthTokenCache cache;

    private final ArmTokenRequest armTokenRequest = ArmTokenRequest.builder()
        .username(USERNAME)
        .password(PASSWORD)
        .build();

    @BeforeEach
    void setUp() {
        cacheManager = new ConcurrentMapCacheManager(ARM_TOKEN_CACHE_NAME);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        armClientService = mock(ArmClientService.class);
        armApiConfigurationProperties = mock(ArmApiConfigurationProperties.class);
        when(armApiConfigurationProperties.getArmServiceProfile()).thenReturn(SERVICE_PROFILE);

        cache = new ArmAuthTokenCacheImpl(cacheManager, redisTemplate, armClientService, armApiConfigurationProperties);
    }

    @Test
    void getToken_ReturnsToken_WhenFirstCallFetchesAndCachesThenSubsequentCallsHitCache() {
        ArmTokenResponse tokenResponse = tokenResponse("bearer-token");
        when(armClientService.getToken(armTokenRequest)).thenReturn(tokenResponse);

        when(armClientService.availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class)))
            .thenReturn(profilesWith(SERVICE_PROFILE, "PID-123"));

        when(armClientService.selectEntitlementProfile(anyString(), eq("PID-123"), any(EmptyRpoRequest.class)))
            .thenReturn(tokenResponse("final-T2"));

        // no lock contention in this simple path
        when(redisTemplate.opsForValue().setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(true);

        // Act
        String token1 = cache.getToken(armTokenRequest);
        String token2 = cache.getToken(armTokenRequest);

        // Assert
        assertThat(token1).isEqualTo("Bearer final-T2");
        assertThat(token2).isEqualTo("Bearer final-T2");

        // Only one full refresh happened; second call read from cache
        verify(armClientService, times(1)).getToken(any(ArmTokenRequest.class));
        verify(armClientService, times(1)).availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class));
        verify(armClientService, times(1)).selectEntitlementProfile(anyString(), anyString(), any(EmptyRpoRequest.class));
    }

    @Test
    void getToken_ReturnsToken_WhenConcurrentMissesResultInSingleRefreshAcrossThreads() throws InterruptedException {
        // Arrange: lock behaviour – first thread gets the lock; others fail to acquire
        when(redisTemplate.opsForValue().setIfAbsent(startsWith("lock:arm-token"), anyString(), any(Duration.class)))
            .thenReturn(true)   // first contender acquires
            .thenReturn(false)  // others see lock held
            .thenReturn(false)
            .thenReturn(false);

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

        // Act: 4 concurrent calls
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

        // Assert
        assertThat(completed).isTrue();
        assertThat(results).containsOnly("Bearer final-T2");
        // Only one refresh path executed
        verify(armClientService, times(4)).getToken(any(ArmTokenRequest.class));
        verify(armClientService, times(4)).availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class));
        verify(armClientService, times(4)).selectEntitlementProfile(anyString(), anyString(), any(EmptyRpoRequest.class));
    }

    @Test
    void getToken_ReturnsToken_WhenTriggersEvictAndRetryOnce_thenSucceeds() {
        // Arrange first oauth call
        when(armClientService.getToken(any(ArmTokenRequest.class)))
            .thenReturn(tokenResponse("token1"), tokenResponse("token2")); // second call during retry path

        // First call to profiles throws 401; second succeeds
        when(armClientService.availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "unauth", new byte[0], StandardCharsets.UTF_8))
            .thenAnswer(inv -> {
                AvailableEntitlementProfile.Profiles p = profilesWith(SERVICE_PROFILE, "PID-123").getProfiles().get(0);
                AvailableEntitlementProfile profs = mock(AvailableEntitlementProfile.class);
                when(profs.isError()).thenReturn(false);
                when(profs.getProfiles()).thenReturn(List.of(p));
                return profs;
            });

        when(armClientService.selectEntitlementProfile(anyString(), eq("PID-123"), any(EmptyRpoRequest.class)))
            .thenReturn(tokenResponse("final-T2"));

        when(redisTemplate.opsForValue().setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(true);

        String token = cache.getToken(armTokenRequest);

        assertThat(token).isEqualTo("Bearer final-T2");

        // Verify that we tried profiles twice (once after getting a fresh OAuth token again)
        verify(armClientService, times(2)).availableEntitlementProfiles(anyString(), any(EmptyRpoRequest.class));
        verify(armClientService, times(2)).getToken(any(ArmTokenRequest.class));
        verify(armClientService, times(1)).selectEntitlementProfile(anyString(), anyString(), any(EmptyRpoRequest.class));
    }

    @Test
    void evictToken_forcesNextCallToRefetch() {
        when(redisTemplate.opsForValue().setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(true);

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

}