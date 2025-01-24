package uk.gov.hmcts.darts.arm.rpo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.ProductionOutputFilesRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.ProductionOutputFilesResponse;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ArmRpoStateEnum;
import uk.gov.hmcts.darts.common.enums.ArmRpoStatusEnum;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.arm.enums.ArmRpoResponseStatusCode.READY_STATUS;

@SuppressWarnings("checkstyle:linelength")
class ArmRpoApiGetProductionOutputFilesIntTest extends PostgresIntegrationBase {

    @MockBean
    private ArmRpoClient armRpoClient;

    @Autowired
    private ArmRpoApi armRpoApi;

    private static final String TOKEN = "some token";
    private static final String PRODUCTION_ID = UUID.randomUUID().toString();
    private static final String PRODUCTION_EXPORT_FILE_ID1 = UUID.randomUUID().toString();
    private static final String PRODUCTION_EXPORT_FILE_ID2 = UUID.randomUUID().toString();

    @Test
    void getProductionOutputFiles_shouldSucceedAndReturnSingleItem_whenSuccessResponseIsReturnedFromArmWithSingularProductionExportFile() {
        // Given
        var productionOutputFilesResponse = createProductionOutputFilesResponse(PRODUCTION_EXPORT_FILE_ID1);
        when(armRpoClient.getProductionOutputFiles(eq(TOKEN), any(ProductionOutputFilesRequest.class)))
            .thenReturn(productionOutputFilesResponse);

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        var executionDetailEntity = createExecutionDetailEntity(userAccount);
        executionDetailEntity = dartsPersistence.save(executionDetailEntity);

        Integer executionId = executionDetailEntity.getId();

        // When
        List<String> productionOutputFiles = armRpoApi.getProductionOutputFiles(TOKEN, executionId, userAccount);

        // Then
        assertEquals(1, productionOutputFiles.size());
        assertEquals(PRODUCTION_EXPORT_FILE_ID1, productionOutputFiles.getFirst());

        executionDetailEntity = dartsPersistence.getArmRpoExecutionDetailRepository().findById(executionId)
            .orElseThrow();
        assertEquals(ArmRpoStateEnum.GET_PRODUCTION_OUTPUT_FILES.getId(), executionDetailEntity.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.COMPLETED.getId(), executionDetailEntity.getArmRpoStatus().getId());
    }

    @Test
    void getProductionOutputFiles_shouldSucceedAndReturnMultipleItems_whenSuccessResponseIsReturnedFromArmWithSingularProductionExportFile() {
        // Given
        var productionOutputFilesResponse = createMultipleProductionOutputFilesResponse(PRODUCTION_EXPORT_FILE_ID1, PRODUCTION_EXPORT_FILE_ID2);
        when(armRpoClient.getProductionOutputFiles(eq(TOKEN), any(ProductionOutputFilesRequest.class)))
            .thenReturn(productionOutputFilesResponse);

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        var executionDetailEntity = createExecutionDetailEntity(userAccount);
        executionDetailEntity = dartsPersistence.save(executionDetailEntity);

        Integer executionId = executionDetailEntity.getId();

        // When
        List<String> productionOutputFiles = armRpoApi.getProductionOutputFiles(TOKEN, executionId, userAccount);

        // Then
        assertEquals(2, productionOutputFiles.size());
        assertEquals(PRODUCTION_EXPORT_FILE_ID1, productionOutputFiles.getFirst());
        assertEquals(PRODUCTION_EXPORT_FILE_ID2, productionOutputFiles.get(1));

        executionDetailEntity = dartsPersistence.getArmRpoExecutionDetailRepository().findById(executionId).orElseThrow();
        assertEquals(ArmRpoStateEnum.GET_PRODUCTION_OUTPUT_FILES.getId(), executionDetailEntity.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.COMPLETED.getId(), executionDetailEntity.getArmRpoStatus().getId());
    }

    @Test
    void getProductionOutputFiles_shouldThrowException_whenArmReturnsNoProductionExportFiles() {
        // Given
        var productionOutputFilesResponse = createProductionOutputFilesResponse(null);
        when(armRpoClient.getProductionOutputFiles(eq(TOKEN), any(ProductionOutputFilesRequest.class)))
            .thenReturn(productionOutputFilesResponse);

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        var executionDetailEntity = dartsPersistence.save(createExecutionDetailEntity(userAccount));
        Integer executionId = executionDetailEntity.getId();

        // When
        String exceptionMessage = assertThrows(ArmRpoException.class, () ->
            armRpoApi.getProductionOutputFiles(TOKEN, executionId, userAccount))
            .getMessage();

        // Then
        assertThat(exceptionMessage, containsString("ARM getProductionOutputFiles: No production export file id's were returned"));

        executionDetailEntity = dartsPersistence.getArmRpoExecutionDetailRepository().findById(executionId)
            .orElseThrow();
        assertEquals(ArmRpoStateEnum.GET_PRODUCTION_OUTPUT_FILES.getId(), executionDetailEntity.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.FAILED.getId(), executionDetailEntity.getArmRpoStatus().getId());
    }

    private ArmRpoExecutionDetailEntity createExecutionDetailEntity(UserAccountEntity userAccount) {
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setCreatedBy(userAccount);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccount);
        armRpoExecutionDetailEntity.setProductionId(PRODUCTION_ID);
        return armRpoExecutionDetailEntity;
    }

    private ProductionOutputFilesResponse createProductionOutputFilesResponse(String fileId) {
        var productionExportFileDetail = new ProductionOutputFilesResponse.ProductionExportFileDetail();
        productionExportFileDetail.setProductionExportFileId(fileId);
        productionExportFileDetail.setStatus(READY_STATUS.getStatusCode());

        var productionExportFile = new ProductionOutputFilesResponse.ProductionExportFile();
        productionExportFile.setProductionExportFileDetails(productionExportFileDetail);

        var response = new ProductionOutputFilesResponse();
        response.setStatus(200);
        response.setIsError(false);
        response.setProductionExportFiles(Collections.singletonList(productionExportFile));

        return response;
    }

    private ProductionOutputFilesResponse createMultipleProductionOutputFilesResponse(String fileId1, String fileId2) {
        var productionExportFileDetail1 = new ProductionOutputFilesResponse.ProductionExportFileDetail();
        productionExportFileDetail1.setProductionExportFileId(fileId1);
        productionExportFileDetail1.setStatus(READY_STATUS.getStatusCode());

        var productionExportFile1 = new ProductionOutputFilesResponse.ProductionExportFile();
        productionExportFile1.setProductionExportFileDetails(productionExportFileDetail1);

        var productionExportFileDetail2 = new ProductionOutputFilesResponse.ProductionExportFileDetail();
        productionExportFileDetail2.setProductionExportFileId(fileId2);
        productionExportFileDetail2.setStatus(READY_STATUS.getStatusCode());

        var productionExportFile2 = new ProductionOutputFilesResponse.ProductionExportFile();
        productionExportFile2.setProductionExportFileDetails(productionExportFileDetail2);

        var response = new ProductionOutputFilesResponse();
        response.setStatus(200);
        response.setIsError(false);
        response.setProductionExportFiles(List.of(productionExportFile1, productionExportFile2));

        return response;
    }

}
