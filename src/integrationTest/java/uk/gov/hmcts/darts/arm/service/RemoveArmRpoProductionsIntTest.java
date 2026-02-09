package uk.gov.hmcts.darts.arm.service;

import feign.FeignException;
import feign.Request;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.arm.client.ArmTokenClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.RemoveProductionResponse;
import uk.gov.hmcts.darts.arm.client.version.fivetwo.ArmApiBaseClient;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.InMemoryTestCache;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getArmRpoExecutionDetailTestData;

@TestPropertySource(properties = {"darts.storage.arm-api.enable-arm-v5-2-upgrade=true"})
@Profile("in-memory-caching")
@Import(InMemoryTestCache.class)
@SpringBootTest
@TestPropertySource(properties = {"darts.storage.arm.is-mock-arm-rpo-download-csv=false"})
@Slf4j
@SuppressWarnings({"PMD.CloseResource"})
class RemoveArmRpoProductionsIntTest extends PostgresIntegrationBase {

    @Autowired
    private ArmApiConfigurationProperties armApiConfigurationProperties;
    @MockitoBean
    private ArmTokenClient armTokenClient;
    @MockitoBean
    private UserIdentity userIdentity;
    @MockitoBean
    private ArmApiBaseClient armApiBaseClient;
    @MockitoBean
    private ArmRpoUtil armRpoUtil;
    
    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity;
    private final Duration  waitDuration = Duration.ofDays(30);

    @Autowired
    private RemoveRpoProductionsService removeRpoProductionsService;
    @Autowired
    private ArmRpoService armRpoService;
    
    @BeforeEach
    void setUp() {
        UserAccountEntity userAccountEntity = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        lenient().when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        armRpoExecutionDetailEntity = dartsPersistence.save(getArmRpoExecutionDetailTestData().minimalArmRpoExecutionDetailEntity());
    }
    
    @Test
    void removeOldArmRpoProductions_ShouldRemoveProductions_WhenFailedAndOlderThanDuration() {
        RemoveProductionResponse response = new RemoveProductionResponse();
        response.setStatus(200);
        response.setIsError(false);
        when(armApiBaseClient.removeProduction(any(), any())).thenReturn(response);
        doReturn("token").when(armRpoUtil).getBearerToken("removeProduction");
        when(armApiBaseClient.removeProduction(eq("token"), any())).thenReturn(response);

        //given
        armRpoExecutionDetailEntity.setArmRpoStatus(ArmRpoHelper.failedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ArmRpoHelper.saveBackgroundSearchRpoState());
        dartsPersistence.save(armRpoExecutionDetailEntity);
        
        // update automatically set lastModifiedDateTime to be older than waitDuration. This defaults to now() on save
        dartsPersistence.getArmRpoExecutionDetailRepository()
            .updateLastModifiedDateTimeById(
                armRpoExecutionDetailEntity.getId(),
                OffsetDateTime.now().minusDays(31)
            );
        
        // when
        removeRpoProductionsService.removeOldArmRpoProductions(false, waitDuration, 10);
        
        // then
        var updatedArmRpoExecutionDetailEntity = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetailEntity.getId());

        assertNotNull(updatedArmRpoExecutionDetailEntity);
        assertEquals(ArmRpoHelper.removeProductionRpoState().getId(), updatedArmRpoExecutionDetailEntity.get().getArmRpoState().getId());
        assertEquals(ArmRpoHelper.completedRpoStatus().getId(), updatedArmRpoExecutionDetailEntity.get().getArmRpoStatus().getId());
        verify(armApiBaseClient).removeProduction(any(), any());
    }

    @Test
    void removeOldArmRpoProductions_ShouldNotRemoveProductions_WhenFailedAndYoungerThanDuration() {
        RemoveProductionResponse response = new RemoveProductionResponse();
        response.setStatus(200);
        response.setIsError(false);
        when(armApiBaseClient.removeProduction(any(), any())).thenReturn(response);
        doReturn("token").when(armRpoUtil).getBearerToken("removeProduction");
        when(armApiBaseClient.removeProduction(eq("token"), any())).thenReturn(response);

        //given
        // create execution detail with FAILED status and last modified date older than duration
        armRpoExecutionDetailEntity.setArmRpoStatus(ArmRpoHelper.failedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ArmRpoHelper.saveBackgroundSearchRpoState());
        dartsPersistence.save(armRpoExecutionDetailEntity);

        // update automatically set lastModifiedDateTime to be older than waitDuration. This defaults to now() on save
        dartsPersistence.getArmRpoExecutionDetailRepository()
            .updateLastModifiedDateTimeById(
                armRpoExecutionDetailEntity.getId(),
                OffsetDateTime.now().minusDays(29)
            );

        // when
        removeRpoProductionsService.removeOldArmRpoProductions(false, waitDuration, 10);

        // then
        var updatedArmRpoExecutionDetailEntity = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetailEntity.getId());

        assertNotNull(updatedArmRpoExecutionDetailEntity);
        assertEquals(ArmRpoHelper.saveBackgroundSearchRpoState().getId(), updatedArmRpoExecutionDetailEntity.get().getArmRpoState().getId());
        assertEquals(ArmRpoHelper.failedRpoStatus().getId(), updatedArmRpoExecutionDetailEntity.get().getArmRpoStatus().getId());
        verify(armApiBaseClient, times(0)).removeProduction(any(), any());
        verifyNoMoreInteractions(armApiBaseClient);
    }

    @Test
    void removeOldArmRpoProductions_ShouldNotRemoveProductions_WhenCompletedAndOlderThanDuration() {
        RemoveProductionResponse response = new RemoveProductionResponse();
        response.setStatus(200);
        response.setIsError(false);
        when(armApiBaseClient.removeProduction(any(), any())).thenReturn(response);
        doReturn("token").when(armRpoUtil).getBearerToken("removeProduction");
        when(armApiBaseClient.removeProduction(eq("token"), any())).thenReturn(response);
        //given
        armRpoExecutionDetailEntity.setArmRpoStatus(ArmRpoHelper.completedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ArmRpoHelper.saveBackgroundSearchRpoState());
        dartsPersistence.save(armRpoExecutionDetailEntity);

        // update automatically set lastModifiedDateTime to be older than waitDuration. This defaults to now() on save
        dartsPersistence.getArmRpoExecutionDetailRepository()
            .updateLastModifiedDateTimeById(
                armRpoExecutionDetailEntity.getId(),
                OffsetDateTime.now().minusDays(31)
            );

        // when
        removeRpoProductionsService.removeOldArmRpoProductions(false, waitDuration, 10);

        // then
        var updatedArmRpoExecutionDetailEntity = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetailEntity.getId());

        assertNotNull(updatedArmRpoExecutionDetailEntity);
        assertEquals(ArmRpoHelper.saveBackgroundSearchRpoState().getId(), updatedArmRpoExecutionDetailEntity.get().getArmRpoState().getId());
        assertEquals(ArmRpoHelper.completedRpoStatus().getId(), updatedArmRpoExecutionDetailEntity.get().getArmRpoStatus().getId());
        verify(armApiBaseClient, times(0)).removeProduction(any(), any());
        verifyNoMoreInteractions(armApiBaseClient);
    }

    @Test
    void removeOldArmRpoProductions_ShouldRetryOnUnauthorisedThenSucceed_WhenFailedAndOlderThanDuration() {
        RemoveProductionResponse response = new RemoveProductionResponse();
        response.setStatus(200);
        response.setIsError(false);
        
        //given
        armRpoExecutionDetailEntity.setArmRpoStatus(ArmRpoHelper.failedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ArmRpoHelper.saveBackgroundSearchRpoState());
        dartsPersistence.save(armRpoExecutionDetailEntity);

        // update automatically set lastModifiedDateTime to be older than waitDuration. This defaults to now() on save
        dartsPersistence.getArmRpoExecutionDetailRepository()
            .updateLastModifiedDateTimeById(
                armRpoExecutionDetailEntity.getId(),
                OffsetDateTime.now().minusDays(31)
            );

        // throw 401 then try again and succeed
        Response unauthorised = Response.builder()
            .request(Request.create(Request.HttpMethod.POST, "/removeProduction", Map.of(), null, StandardCharsets.UTF_8, null))
            .status(401)
            .reason("Unauthorized")
            .build();
        FeignException feign401 = FeignException.errorStatus("removeProduction", unauthorised);
        // when armRpoUtil.getBearerToken is called first time, return initial bearer
        doReturn("token").when(armRpoUtil).getBearerToken("removeProduction");
        // when armRpoUtil.retryGetBearerToken is called, return refreshed bearer
        doReturn("Bearer refreshed").when(armRpoUtil).retryGetBearerToken("removeProduction");

        when(armApiBaseClient.removeProduction(eq("token"), any())).thenThrow(feign401);
        when(armApiBaseClient.removeProduction(eq("Bearer refreshed"), any())).thenReturn(response);

        // when
        removeRpoProductionsService.removeOldArmRpoProductions(false, waitDuration, 10);

        // then
        var updatedArmRpoExecutionDetailEntity = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetailEntity.getId());

        assertNotNull(updatedArmRpoExecutionDetailEntity);
        assertEquals(ArmRpoHelper.removeProductionRpoState().getId(), updatedArmRpoExecutionDetailEntity.get().getArmRpoState().getId());
        assertEquals(ArmRpoHelper.completedRpoStatus().getId(), updatedArmRpoExecutionDetailEntity.get().getArmRpoStatus().getId());
        verify(armApiBaseClient, times(2)).removeProduction(any(), any());
    }

    @Test
    void removeOldArmRpoProductions_ShouldRetryOnUnauthorisedThenFailSecondUnauthorised_WhenFailedAndOlderThanDuration() {
        when(armRpoUtil.handleFailureAndCreateException(anyString(), any(), any()))
            .thenAnswer(invocation -> {
                String msg = invocation.getArgument(0);
                ArmRpoExecutionDetailEntity entity = invocation.getArgument(1);
                UserAccountEntity user = invocation.getArgument(2);
                armRpoService.updateArmRpoStatus(entity, ArmRpoHelper.failedRpoStatus(), user);
                return new ArmRpoException(msg);
            });
        RemoveProductionResponse response = new RemoveProductionResponse();
        response.setStatus(200);
        response.setIsError(false);
        
        armRpoExecutionDetailEntity.setArmRpoStatus(ArmRpoHelper.failedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ArmRpoHelper.saveBackgroundSearchRpoState());
        dartsPersistence.save(armRpoExecutionDetailEntity);

        // update automatically set lastModifiedDateTime to be older than waitDuration. This defaults to now() on save
        dartsPersistence.getArmRpoExecutionDetailRepository()
            .updateLastModifiedDateTimeById(
                armRpoExecutionDetailEntity.getId(),
                OffsetDateTime.now().minusDays(31)
            );

        // throw 401 then try again and succeed
        Response unauthorised = Response.builder()
            .request(Request.create(Request.HttpMethod.POST, "/removeProduction", Map.of(), null, StandardCharsets.UTF_8, null))
            .status(401)
            .reason("Unauthorized")
            .build();
        FeignException feign401 = FeignException.errorStatus("removeProduction", unauthorised);


        // when armRpoUtil.getBearerToken is called first time, return initial bearer
        doReturn("token").when(armRpoUtil).getBearerToken("removeProduction");
        // when armRpoUtil.retryGetBearerToken is called, return refreshed bearer
        doReturn("Bearer refreshed").when(armRpoUtil).retryGetBearerToken("removeProduction");

        when(armApiBaseClient.removeProduction(eq("token"), any())).thenThrow(feign401);

        when(armApiBaseClient.removeProduction(eq("Bearer refreshed"), any())).thenThrow(feign401);

        // when
        removeRpoProductionsService.removeOldArmRpoProductions(false, waitDuration, 10);

        // then
        var updatedArmRpoExecutionDetailEntity = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetailEntity.getId());

        assertNotNull(updatedArmRpoExecutionDetailEntity);
        assertEquals(ArmRpoHelper.removeProductionRpoState().getId(), updatedArmRpoExecutionDetailEntity.get().getArmRpoState().getId());
        assertEquals(ArmRpoHelper.failedRpoStatus().getId(), updatedArmRpoExecutionDetailEntity.get().getArmRpoStatus().getId());
        verify(armApiBaseClient, atLeastOnce()).removeProduction(any(), any());
    }
}
