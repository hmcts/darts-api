package uk.gov.hmcts.darts.arm.rpo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.CreateExportBasedOnSearchResultsTableResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.MasterIndexFieldByRecordClassSchemaResponse;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ArmRpoStateEnum;
import uk.gov.hmcts.darts.common.enums.ArmRpoStatusEnum;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class ArmRpoApiCreateExportBasedOnSearchResultsTableIntTest extends PostgresIntegrationBase {
    @MockBean
    private ArmRpoClient armRpoClient;

    @Autowired
    private ArmRpoApi armRpoApi;


    @Test
    void createExportBasedOnSearchResultsTableSuccess() {
        // given
        CreateExportBasedOnSearchResultsTableResponse response = new CreateExportBasedOnSearchResultsTableResponse();
        response.setStatus(200);
        response.setIsError(false);
        response.setResponseStatus(0);
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenReturn(response);

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setCreatedBy(userAccount);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccount);
        armRpoExecutionDetailEntity.setSearchId("searchId");
        armRpoExecutionDetailEntity.setSearchItemCount(6);
        armRpoExecutionDetailEntity.setProductionId("productionId");
        armRpoExecutionDetailEntity.setStorageAccountId("storageAccountId");
        var armRpoExecutionDetail = dartsPersistence.save(armRpoExecutionDetailEntity);

        var bearerAuth = "Bearer some-token";

        // when
        boolean result = armRpoApi.createExportBasedOnSearchResultsTable(
            bearerAuth, armRpoExecutionDetail.getId(), createHeaderColumns(), userAccount);

        // then
        assertTrue(result);

        var armRpoExecutionDetailEntityUpdated = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetail.getId()).orElseThrow();
        assertEquals(ArmRpoStateEnum.CREATE_EXPORT_BASED_ON_SEARCH_RESULTS_TABLE.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.COMPLETED.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoStatus().getId());

    }


    private List<MasterIndexFieldByRecordClassSchemaResponse> createHeaderColumns() {
        MasterIndexFieldByRecordClassSchemaResponse response = new MasterIndexFieldByRecordClassSchemaResponse();
        response.setMasterIndexFields(List.of(
            createMasterIndexFieldByRecordClassSchemaResponse("200b9c27-b497-4977-82e7-1586b32a5871", "Record Class", "record_class", "string", false),
            createMasterIndexFieldByRecordClassSchemaResponse("90ee0e13-8639-4c4a-b542-66b6c8911549", "Archived Date", "ingestionDate", "date", false),
            createMasterIndexFieldByRecordClassSchemaResponse("a9b8daf2-d9ff-4815-b65a-f6ae2763b92c", "Client Identifier", "client_identifier", "string",
                                                              false),
            createMasterIndexFieldByRecordClassSchemaResponse("109b6bf1-57a0-48ec-b22e-c7248dc74f91", "Contributor", "contributor", "string", false),
            createMasterIndexFieldByRecordClassSchemaResponse("893048bf-1e7c-4811-9abf-00cd77a715cf", "Record Date", "recordDate", "date", false),
            createMasterIndexFieldByRecordClassSchemaResponse("fdd0fcbb-da46-4af1-a627-ac255c12bb23", "ObjectId", "bf_012", "number", false)
        ));
        return List.of(response);
    }

    private MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField createMasterIndexFieldByRecordClassSchemaResponse(
        String uuid, String displayName, String propertyName, String propertyType, boolean isMasked) {

        MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField masterIndexField = new MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField();
        masterIndexField.setMasterIndexFieldId(uuid);
        masterIndexField.setDisplayName(displayName);
        masterIndexField.setPropertyName(propertyName);
        masterIndexField.setPropertyType(propertyType);
        masterIndexField.setIsMasked(isMasked);
        return masterIndexField;
    }
}
