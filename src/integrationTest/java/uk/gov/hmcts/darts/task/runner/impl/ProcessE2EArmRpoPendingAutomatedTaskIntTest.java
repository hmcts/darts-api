package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.ArmAsyncSearchResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.IndexesByMatterIdResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.MasterIndexFieldByRecordClassSchemaResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.ProfileEntitlementResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.RecordManagementMatterResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.SaveBackgroundSearchResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.StorageAccountResponse;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.service.ArmApiService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ArmRpoStateEnum;
import uk.gov.hmcts.darts.common.enums.ArmRpoStatusEnum;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@TestPropertySource(properties = {
    "darts.storage.arm-api.arm-storage-account-name=SOME ARM STORAGE ACCOUNT NAME",
    "darts.storage.arm-api.arm-service-entitlement=SOME ENTITLEMENT NAME"
})
class ProcessE2EArmRpoPendingAutomatedTaskIntTest extends PostgresIntegrationBase {

    @Autowired
    private ProcessE2EArmRpoPendingAutomatedTask task;

    @MockBean
    private UserIdentity userIdentity;

    @MockBean
    private ArmApiService armApiService;

    @MockBean
    private ArmRpoClient armRpoClient;

    private static final String BEARER_TOKEN = "SOME BEARER TOKEN";
    private static final String MATTER_ID = "SOME MATTER ID";
    private static final String INDEX_ID = "SOME INDEX ID";
    private static final String STORAGE_ACCOUNT_INDEX_NAME = "SOME ARM STORAGE ACCOUNT NAME";
    private static final String STORAGE_ACCOUNT_INDEX_ID = "SOME ARM STORAGE ACCOUNT INDEX ID";
    private static final String ENTITLEMENT_NAME = "SOME ENTITLEMENT NAME";
    private static final String ENTITLEMENT_ID = "SOME ENTITLEMENT ID";
    private static final String SOME_MASTER_INDEX_FIELD_ID = "SOME MASTER INDEX FIELD ID";
    private static final String SEARCH_ID = "SOME SEARCH ID";

    @BeforeEach
    void setUp() {
        UserAccountEntity user = dartsDatabase.getUserAccountStub().getSystemUserAccountEntity();
        when(userIdentity.getUserAccount())
            .thenReturn(user);

        when(armApiService.getArmBearerToken())
            .thenReturn(BEARER_TOKEN);
    }

    @Test
    void runTask_shouldTriggerArmRpoSearch_andSetExpectedFinalExecutionDetailStateAndStatus_whenAllDownstreamArmCallsAreSuccessful() {
        // Given
        createAndSetRecordManagementMatterMock();
        createAndSetIndexesByMatterIdMock();
        createAndSetGetStorageAccountsMock();
        createAndSetGetProfileEntitlementsMock();
        createAndSetGetMasterIndexFieldByRecordClassSchemaMock();
        createAndSetAddAsyncSearchMock();
        createAndSetSaveBackgroundSearchMock();

        // When
        task.runTask();

        // Then
        List<ArmRpoExecutionDetailEntity> allExecutionDetails = dartsDatabase.getArmRpoExecutionDetailRepository()
            .findAll();
        assertEquals(1, allExecutionDetails.size());

        var executionDetail = allExecutionDetails.getFirst();
        assertEquals(ArmRpoStateEnum.SAVE_BACKGROUND_SEARCH.getId(), executionDetail.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.COMPLETED.getId(), executionDetail.getArmRpoStatus().getId());

        assertEquals(MATTER_ID, executionDetail.getMatterId());
        assertEquals(INDEX_ID, executionDetail.getIndexId());
        assertEquals(ENTITLEMENT_ID, executionDetail.getEntitlementId());
        assertEquals(STORAGE_ACCOUNT_INDEX_ID, executionDetail.getStorageAccountId());
        assertNull(executionDetail.getProductionId());
        assertEquals(SOME_MASTER_INDEX_FIELD_ID, executionDetail.getSortingField());
        assertNull(executionDetail.getSearchItemCount());
    }

    @Test
    void runTask_shouldThrowException_andSetExpectedExecutionDetailStateAndStatus_whenAnyDownstreamArmCallFails() {
        // Given the first API call fails
        var response = new RecordManagementMatterResponse();
        response.setIsError(true);
        response.setStatus(400);

        when(armRpoClient.getRecordManagementMatter(eq(BEARER_TOKEN)))
            .thenReturn(response);

        // When
        ArmRpoException exception = assertThrows(ArmRpoException.class, () -> task.runTask());
        assertThat(exception.getMessage(),
                   containsString("Failure during ARM RPO getRecordManagementMatter: ARM RPO API failed with status - 400 BAD_REQUEST"));

        // Then
        List<ArmRpoExecutionDetailEntity> allExecutionDetails = dartsDatabase.getArmRpoExecutionDetailRepository()
            .findAll();
        assertEquals(1, allExecutionDetails.size());

        var executionDetail = allExecutionDetails.getFirst();
        assertEquals(ArmRpoStateEnum.GET_RECORD_MANAGEMENT_MATTER.getId(), executionDetail.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.FAILED.getId(), executionDetail.getArmRpoStatus().getId());
    }

    private void createAndSetSaveBackgroundSearchMock() {
        SaveBackgroundSearchResponse saveBackgroundSearchResponse = new SaveBackgroundSearchResponse();
        saveBackgroundSearchResponse.setStatus(200);
        saveBackgroundSearchResponse.setIsError(false);

        when(armRpoClient.saveBackgroundSearch(eq(BEARER_TOKEN), any()))
            .thenReturn(saveBackgroundSearchResponse);
    }

    private void createAndSetAddAsyncSearchMock() {
        var response = new ArmAsyncSearchResponse();
        response.setStatus(200);
        response.setIsError(false);
        response.setSearchId(SEARCH_ID);

        when(armRpoClient.addAsyncSearch(eq(BEARER_TOKEN), anyString()))
            .thenReturn(response);
    }

    private void createAndSetGetMasterIndexFieldByRecordClassSchemaMock() {
        var masterIndexField = new MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField();
        masterIndexField.setPropertyName("ingestionDate");
        masterIndexField.setMasterIndexFieldId(SOME_MASTER_INDEX_FIELD_ID);

        var response = new MasterIndexFieldByRecordClassSchemaResponse();
        response.setMasterIndexFields(Collections.singletonList(masterIndexField));
        response.setStatus(200);
        response.setIsError(false);

        when(armRpoClient.getMasterIndexFieldByRecordClassSchema(eq(BEARER_TOKEN), any()))
            .thenReturn(response);
    }

    private void createAndSetGetProfileEntitlementsMock() {
        var profileEntitlement = new ProfileEntitlementResponse.ProfileEntitlement();
        profileEntitlement.setName(ENTITLEMENT_NAME);
        profileEntitlement.setEntitlementId(ENTITLEMENT_ID);

        var response = new ProfileEntitlementResponse();
        response.setEntitlements(Collections.singletonList(profileEntitlement));
        response.setStatus(200);
        response.setIsError(false);

        when(armRpoClient.getProfileEntitlementResponse(BEARER_TOKEN))
            .thenReturn(response);
    }

    private void createAndSetGetStorageAccountsMock() {
        var dataDetails = new StorageAccountResponse.DataDetails();
        dataDetails.setName(STORAGE_ACCOUNT_INDEX_NAME);
        dataDetails.setId(STORAGE_ACCOUNT_INDEX_ID);

        var response = new StorageAccountResponse();
        response.setDataDetails(Collections.singletonList(dataDetails));
        response.setIsError(false);
        response.setStatus(200);

        when(armRpoClient.getStorageAccounts(eq(BEARER_TOKEN), any()))
            .thenReturn(response);
    }

    private void createAndSetIndexesByMatterIdMock() {
        var indexDetails = new IndexesByMatterIdResponse.IndexDetails();
        indexDetails.setIndexId(INDEX_ID);

        var index = new IndexesByMatterIdResponse.Index();
        index.setIndex(indexDetails);

        var response = new IndexesByMatterIdResponse();
        response.setIndexes(Collections.singletonList(index));
        response.setIsError(false);
        response.setStatus(200);

        when(armRpoClient.getIndexesByMatterId(eq(BEARER_TOKEN), any()))
            .thenReturn(response);
    }

    private void createAndSetRecordManagementMatterMock() {
        var recordManagementMatter = new RecordManagementMatterResponse.RecordManagementMatter();
        recordManagementMatter.setMatterId(MATTER_ID);

        var response = new RecordManagementMatterResponse();
        response.setRecordManagementMatter(recordManagementMatter);
        response.setIsError(false);
        response.setStatus(200);

        when(armRpoClient.getRecordManagementMatter(BEARER_TOKEN))
            .thenReturn(response);
    }

}
