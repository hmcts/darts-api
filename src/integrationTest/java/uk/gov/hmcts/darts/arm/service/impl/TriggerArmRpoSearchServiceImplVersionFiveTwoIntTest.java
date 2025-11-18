package uk.gov.hmcts.darts.arm.service.impl;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenResponse;
import uk.gov.hmcts.darts.arm.client.model.AvailableEntitlementProfile;
import uk.gov.hmcts.darts.arm.client.model.rpo.ArmAsyncSearchResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.EmptyRpoRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.IndexesByMatterIdResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.MasterIndexFieldByRecordClassSchemaResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.ProfileEntitlementResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.RecordManagementMatterResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.SaveBackgroundSearchResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.StorageAccountResponse;
import uk.gov.hmcts.darts.arm.client.version.fivetwo.ArmApiBaseClient;
import uk.gov.hmcts.darts.arm.client.version.fivetwo.ArmAuthClient;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@TestPropertySource(properties = {
    "darts.storage.arm-api.enable-arm-v5-2-upgrade=true"
})
class TriggerArmRpoSearchServiceImplVersionFiveTwoIntTest extends IntegrationBase {

    @MockitoBean
    private UserIdentity userIdentity;
    @MockitoBean
    private ArmApiBaseClient armApiBaseClient;
    @MockitoBean
    private ArmAuthClient armAuthClient;

    @Autowired
    private TriggerArmRpoSearchServiceImpl triggerArmRpoSearchServiceImpl;

    @BeforeEach
    void setUp() {
        UserAccountEntity userAccountEntity = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        lenient().when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        ArmTokenRequest armTokenRequest = ArmTokenRequest.builder()
            .username("some-username")
            .password("some-password")
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

    @Test
    void triggerArmRpoSearch_shouldCompleteSuccessfully() {

        // given
        RecordManagementMatterResponse recordManagementMatterResponse = getRecordManagementMatterResponse();
        when(armApiBaseClient.getRecordManagementMatter(anyString(), any()))
            .thenReturn(recordManagementMatterResponse);
        when(armApiBaseClient.getIndexesByMatterId(anyString(), any()))
            .thenReturn(getIndexesByMatterIdResponse());
        when(armApiBaseClient.getStorageAccounts(anyString(), any()))
            .thenReturn(getStorageAccounts());
        when(armApiBaseClient.getProfileEntitlementResponse(anyString(), any()))
            .thenReturn(getProfileEntitlementResponse());
        when(armApiBaseClient.getMasterIndexFieldByRecordClassSchema(anyString(), any()))
            .thenReturn(getMasterIndexFieldByRecordClassSchemaResponse("propertyName1", "propertyName2"));
        when(armApiBaseClient.addAsyncSearch(anyString(), any()))
            .thenReturn(getAsyncSearchResponse());
        when(armApiBaseClient.saveBackgroundSearch(anyString(), any()))
            .thenReturn(getSaveBackgroundSearchResponse());

        // when
        triggerArmRpoSearchServiceImpl.triggerArmRpoSearch(Duration.of(1, ChronoUnit.SECONDS));

        // then
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = dartsPersistence.getArmRpoExecutionDetailRepository()
            .findLatestByCreatedDateTimeDesc().orElseThrow();

        assertNotNull(armRpoExecutionDetailEntity.getId());

    }

    private SaveBackgroundSearchResponse getSaveBackgroundSearchResponse() {
        SaveBackgroundSearchResponse saveBackgroundSearchResponse = new SaveBackgroundSearchResponse();
        saveBackgroundSearchResponse.setStatus(200);
        saveBackgroundSearchResponse.setIsError(false);
        return saveBackgroundSearchResponse;
    }

    private ArmAsyncSearchResponse getAsyncSearchResponse() {
        ArmAsyncSearchResponse response = new ArmAsyncSearchResponse();
        response.setStatus(200);
        response.setIsError(false);
        response.setSearchId("SEARCH_ID");
        return response;
    }

    private @NotNull MasterIndexFieldByRecordClassSchemaResponse getMasterIndexFieldByRecordClassSchemaResponse(String propertyName1,
                                                                                                                String propertyName2) {
        MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField masterIndexField1 = getMasterIndexField1(propertyName1);

        MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField masterIndexField2 = getMasterIndexField2(propertyName2);

        MasterIndexFieldByRecordClassSchemaResponse response = new MasterIndexFieldByRecordClassSchemaResponse();
        response.setMasterIndexFields(List.of(masterIndexField1, masterIndexField2));
        return response;
    }

    private static MasterIndexFieldByRecordClassSchemaResponse.@NotNull MasterIndexField getMasterIndexField2(String propertyName2) {
        MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField masterIndexField2 = new MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField();
        masterIndexField2.setMasterIndexFieldId("2");
        masterIndexField2.setDisplayName("displayName");
        masterIndexField2.setPropertyName(propertyName2);
        masterIndexField2.setPropertyType("propertyType");
        masterIndexField2.setIsMasked(false);
        return masterIndexField2;
    }

    private static MasterIndexFieldByRecordClassSchemaResponse.@NotNull MasterIndexField getMasterIndexField1(String propertyName1) {
        MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField masterIndexField1 = new MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField();
        masterIndexField1.setMasterIndexFieldId("1");
        masterIndexField1.setDisplayName("displayName");
        masterIndexField1.setPropertyName(propertyName1);
        masterIndexField1.setPropertyType("propertyType");
        masterIndexField1.setIsMasked(true);
        return masterIndexField1;
    }

    private ProfileEntitlementResponse getProfileEntitlementResponse() {
        ProfileEntitlementResponse.ProfileEntitlement profileEntitlement = new ProfileEntitlementResponse.ProfileEntitlement();
        profileEntitlement.setName("ENTITLEMENT_NAME");
        profileEntitlement.setEntitlementId("ENTITLEMENT_ID");

        ProfileEntitlementResponse profileEntitlementResponse = new ProfileEntitlementResponse();
        profileEntitlementResponse.setStatus(200);
        profileEntitlementResponse.setIsError(false);
        profileEntitlementResponse.setEntitlements(List.of(profileEntitlement));
        return profileEntitlementResponse;
    }

    private StorageAccountResponse getStorageAccounts() {
        StorageAccountResponse.DataDetails dataDetails1 = new StorageAccountResponse.DataDetails();
        dataDetails1.setId("indexId1");
        dataDetails1.setName("unexpectedAccountName");

        StorageAccountResponse.DataDetails dataDetails2 = new StorageAccountResponse.DataDetails();
        dataDetails2.setId("indexId2");
        dataDetails2.setName("some-account-name");

        StorageAccountResponse storageAccountResponse = new StorageAccountResponse();
        storageAccountResponse.setStatus(200);
        storageAccountResponse.setIsError(false);
        storageAccountResponse.setDataDetails(List.of(dataDetails1, dataDetails2));
        return storageAccountResponse;
    }

    private IndexesByMatterIdResponse getIndexesByMatterIdResponse() {
        IndexesByMatterIdResponse response = new IndexesByMatterIdResponse();
        response.setStatus(200);
        response.setIsError(false);

        IndexesByMatterIdResponse.Index index = new IndexesByMatterIdResponse.Index();
        IndexesByMatterIdResponse.IndexDetails indexDetails = new IndexesByMatterIdResponse.IndexDetails();
        indexDetails.setIndexId("indexId");
        index.setIndexDetails(indexDetails);
        response.setIndexes(List.of(index));
        return response;
    }

    private RecordManagementMatterResponse getRecordManagementMatterResponse() {
        RecordManagementMatterResponse response = new RecordManagementMatterResponse();
        response.setStatus(200);
        response.setIsError(false);
        response.setRecordManagementMatter(new RecordManagementMatterResponse.RecordManagementMatter());
        response.getRecordManagementMatter().setMatterId("some-matter-id");
        return response;
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

    private ArmTokenResponse getArmTokenResponse() {
        return ArmTokenResponse.builder()
            .accessToken("some-token")
            .tokenType("Bearer")
            .expiresIn("3600")
            .build();
    }

}
