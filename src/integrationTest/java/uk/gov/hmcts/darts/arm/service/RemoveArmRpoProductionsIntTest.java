package uk.gov.hmcts.darts.arm.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.ArmTokenClient;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenResponse;
import uk.gov.hmcts.darts.arm.client.model.AvailableEntitlementProfile;
import uk.gov.hmcts.darts.arm.client.model.rpo.EmptyRpoRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.RemoveProductionResponse;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getArmRpoExecutionDetailTestData;

@SpringBootTest
@TestPropertySource(properties = {"darts.storage.arm.is-mock-arm-rpo-download-csv=false"})
@Slf4j
class RemoveArmRpoProductionsIntTest extends PostgresIntegrationBase {

    private static final String PRODUCTION_ID = " b52268a3-75e5-4dd4-a8d3-0b43781cfcf9";
    private static final String SEARCH_ID = "8271f101-8c14-4c41-8865-edc5d8baed99";
    private static final String MATTER_ID = "cb70c7fa-8972-4400-af1d-ff5dd76d2104";
    private static final String STORAGE_ACCOUNT_ID = "StorageAccountId";

    @Autowired
    private ArmApiConfigurationProperties armApiConfigurationProperties;
    @MockitoBean
    private ArmTokenClient armTokenClient;
    @MockitoBean
    private UserIdentity userIdentity;
    @MockitoBean
    private ArmRpoClient armRpoClient;

    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity;
    private final Duration  waitDuration = Duration.ofDays(30);  

    @Autowired
    private RemoveRpoProductionsService removeRpoProductionsService;
    
    @BeforeEach
    void setUp() {

        RemoveProductionResponse response = new RemoveProductionResponse();
        response.setStatus(200);
        response.setIsError(false);
        when(armRpoClient.removeProduction(any(), any())).thenReturn(response);
        
        // TODO rework bearer token after DMP-5303 is done
        String bearerToken = "bearer";
        ArmTokenRequest tokenRequest = ArmTokenRequest.builder()
            .username(armApiConfigurationProperties.getArmUsername())
            .password(armApiConfigurationProperties.getArmPassword())
            .build();
        ArmTokenResponse tokenResponse = ArmTokenResponse.builder().accessToken(bearerToken).build();
        when(armTokenClient.getToken(tokenRequest)).thenReturn(tokenResponse);

        String armProfileId = "profileId";
        AvailableEntitlementProfile profile = AvailableEntitlementProfile.builder()
            .profiles(List.of(AvailableEntitlementProfile.Profiles.builder()
                                  .profileId(armProfileId)
                                  .profileName(armApiConfigurationProperties.getArmServiceProfile())
                                  .build()))
            .build();
        EmptyRpoRequest emptyRpoRequest = EmptyRpoRequest.builder().build();
        when(armTokenClient.availableEntitlementProfiles("Bearer " + bearerToken, emptyRpoRequest)).thenReturn(profile);
        when(armTokenClient.selectEntitlementProfile("Bearer " + bearerToken, armProfileId, emptyRpoRequest)).thenReturn(tokenResponse);

        UserAccountEntity userAccountEntity = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        lenient().when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        armRpoExecutionDetailEntity = dartsPersistence.save(getArmRpoExecutionDetailTestData().minimalArmRpoExecutionDetailEntity());
    }
    
    @Test
    void removeOldArmRpoProductions_ShouldRemoveProductions_WhenFailedAndOlderThanDuration() {
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
        verify(armRpoClient).removeProduction(any(), any());
        verifyNoMoreInteractions(armRpoClient);
    }

    @Test
    void removeOldArmRpoProductions_ShouldNotRemoveProductions_WhenFailedAndYoungerThanDuration() {
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
        verify(armRpoClient, times(0)).removeProduction(any(), any());
        verifyNoMoreInteractions(armRpoClient);
    }

    @Test
    void removeOldArmRpoProductions_ShouldNotRemoveProductions_WhenCompletedAndOlderThanDuration() {
        //given
        armRpoExecutionDetailEntity.setArmRpoStatus(ArmRpoHelper.completedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ArmRpoHelper.saveBackgroundSearchRpoState());
        // log the entity before saving for debugging
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
        verify(armRpoClient, times(0)).removeProduction(any(), any());
        verifyNoMoreInteractions(armRpoClient);
    }
    
    
}
