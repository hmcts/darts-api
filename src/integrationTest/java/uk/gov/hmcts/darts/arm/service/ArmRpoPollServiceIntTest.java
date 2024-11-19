package uk.gov.hmcts.darts.arm.service;

import feign.Request;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.CreateExportBasedOnSearchResultsTableResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.ExtendedProductionsByMatterResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.ExtendedSearchesByMatterResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.MasterIndexFieldByRecordClassSchemaResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.ProductionOutputFilesResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.RemoveProductionResponse;
import uk.gov.hmcts.darts.arm.component.ArmRpoDownloadProduction;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.service.impl.ArmApiServiceImpl;
import uk.gov.hmcts.darts.arm.service.impl.ArmRpoPollServiceImpl;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getArmRpoExecutionDetailTestData;

@TestPropertySource(properties = {"darts.storage.arm.is_mock_arm_rpo_download_csv=false"})
@Slf4j
@SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.CloseResource"})
class ArmRpoPollServiceIntTest extends PostgresIntegrationBase {

    private static final String BEARER_TOKEN = "BearerToken";
    private static final String PRODUCTIONEXPORTFILE_CSV = "tests/arm/service/ArmRpoPollServiceTest/productionexportfile.csv";
    private static final String PRODUCTION_ID = "4";

    @MockBean
    private UserIdentity userIdentity;
    @MockBean
    private ArmRpoClient armRpoClient;
    @MockBean
    private ArmApiServiceImpl armApiService;
    @MockBean
    private ArmRpoDownloadProduction armRpoDownloadProduction;

    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity;

    @Autowired
    private ArmRpoPollServiceImpl armRpoPollService;

    @BeforeEach
    void setUp() {

        UserAccountEntity userAccountEntity = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        lenient().when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        armRpoExecutionDetailEntity = dartsPersistence.save(getArmRpoExecutionDetailTestData().minimalArmRpoExecutionDetailEntity());
    }

    @Test
    void pollArmRpo_shouldPollSuccessfullyWithSaveBackgroundCompleted() throws IOException {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ArmRpoHelper.completedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ArmRpoHelper.saveBackgroundSearchRpoState());
        armRpoExecutionDetailEntity.setMatterId("MatterId");
        armRpoExecutionDetailEntity.setSearchId("SearchId");
        armRpoExecutionDetailEntity.setStorageAccountId("StorageAccountId");
        armRpoExecutionDetailEntity.setProductionId(PRODUCTION_ID);
        armRpoExecutionDetailEntity = dartsPersistence.save(armRpoExecutionDetailEntity);

        when(armApiService.getArmBearerToken()).thenReturn(BEARER_TOKEN);
        when(armRpoClient.getExtendedSearchesByMatter(any(), any()))
            .thenReturn(getExtendedSearchesByMatterResponse());
        when(armRpoClient.getMasterIndexFieldByRecordClassSchema(any(), any()))
            .thenReturn(getMasterIndexFieldByRecordClassSchemaResponse("propertyName", "ingestionDate"));
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any()))
            .thenReturn(getCreateExportBasedOnSearchResultsTableResponse());
        when(armRpoClient.getExtendedProductionsByMatter(anyString(), any()))
            .thenReturn(getExtendedProductionsByMatterResponse());
        when(armRpoClient.getProductionOutputFiles(any(), any()))
            .thenReturn(getProductionOutputFilesResponse(PRODUCTION_ID));

        when(armRpoDownloadProduction.downloadProduction(any(), any(), any()))
            .thenReturn(getFeignResponse(200));

        when(armRpoClient.removeProduction(any(), any()))
            .thenReturn(getRemoveProductionResponse());

        // when
        armRpoPollService.pollArmRpo(false);

        // then
        var updatedArmRpoExecutionDetailEntity = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetailEntity.getId());
        assertNotNull(updatedArmRpoExecutionDetailEntity);
        assertEquals(ArmRpoHelper.removeProductionRpoState().getId(), updatedArmRpoExecutionDetailEntity.get().getArmRpoState().getId());
        assertEquals(ArmRpoHelper.completedRpoStatus().getId(), updatedArmRpoExecutionDetailEntity.get().getArmRpoStatus().getId());

        verify(armRpoClient).getExtendedSearchesByMatter(any(), any());
        verify(armRpoClient).getMasterIndexFieldByRecordClassSchema(any(), any());
        verify(armRpoClient).createExportBasedOnSearchResultsTable(anyString(), any());
        verify(armRpoClient).getExtendedProductionsByMatter(anyString(), any());
        verify(armRpoClient).getProductionOutputFiles(any(), any());
        verify(armRpoDownloadProduction).downloadProduction(any(), any(), any());
        verify(armRpoClient).removeProduction(any(), any());
        verifyNoMoreInteractions(armRpoClient);

    }

    @Test
    void pollArmRpo_shouldPollSuccessfullyWithSaveBackgroundCompletedCreateExportInProgress() throws IOException {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ArmRpoHelper.completedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ArmRpoHelper.saveBackgroundSearchRpoState());
        armRpoExecutionDetailEntity.setMatterId("MatterId");
        armRpoExecutionDetailEntity.setSearchId("SearchId");
        armRpoExecutionDetailEntity.setStorageAccountId("StorageAccountId");
        armRpoExecutionDetailEntity.setProductionId(PRODUCTION_ID);
        armRpoExecutionDetailEntity = dartsPersistence.save(armRpoExecutionDetailEntity);

        when(armApiService.getArmBearerToken()).thenReturn(BEARER_TOKEN);
        when(armRpoClient.getExtendedSearchesByMatter(any(), any()))
            .thenReturn(getExtendedSearchesByMatterResponse());
        when(armRpoClient.getMasterIndexFieldByRecordClassSchema(any(), any()))
            .thenReturn(getMasterIndexFieldByRecordClassSchemaResponse("propertyName", "ingestionDate"));
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any()))
            .thenReturn(getCreateExportBasedOnSearchResultsTableResponseInProgress());

        // when
        armRpoPollService.pollArmRpo(false);

        // then
        var updatedArmRpoExecutionDetailEntity = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetailEntity.getId());
        assertNotNull(updatedArmRpoExecutionDetailEntity);
        assertEquals(ArmRpoHelper.createExportBasedOnSearchResultsTableRpoState().getId(), updatedArmRpoExecutionDetailEntity.get().getArmRpoState().getId());
        assertEquals(ArmRpoHelper.inProgressRpoStatus().getId(), updatedArmRpoExecutionDetailEntity.get().getArmRpoStatus().getId());

        verify(armRpoClient).getExtendedSearchesByMatter(any(), any());
        verify(armRpoClient).getMasterIndexFieldByRecordClassSchema(any(), any());
        verify(armRpoClient).createExportBasedOnSearchResultsTable(anyString(), any());
        verifyNoMoreInteractions(armRpoClient);

    }

    @Test
    void pollArmRpo_shouldPollSuccessfullyWithFailedDownloadProductionIsManual() throws IOException {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ArmRpoHelper.failedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ArmRpoHelper.downloadProductionRpoState());
        armRpoExecutionDetailEntity.setMatterId("MatterId");
        armRpoExecutionDetailEntity.setSearchId("SearchId");
        armRpoExecutionDetailEntity.setStorageAccountId("StorageAccountId");
        armRpoExecutionDetailEntity.setProductionId(PRODUCTION_ID);
        armRpoExecutionDetailEntity = dartsPersistence.save(armRpoExecutionDetailEntity);

        when(armApiService.getArmBearerToken()).thenReturn(BEARER_TOKEN);
        when(armRpoClient.getExtendedSearchesByMatter(any(), any()))
            .thenReturn(getExtendedSearchesByMatterResponse());
        when(armRpoClient.getMasterIndexFieldByRecordClassSchema(any(), any()))
            .thenReturn(getMasterIndexFieldByRecordClassSchemaResponse("propertyName", "ingestionDate"));
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any()))
            .thenReturn(getCreateExportBasedOnSearchResultsTableResponse());
        when(armRpoClient.getExtendedProductionsByMatter(anyString(), any()))
            .thenReturn(getExtendedProductionsByMatterResponse());
        when(armRpoClient.getProductionOutputFiles(any(), any()))
            .thenReturn(getProductionOutputFilesResponse(PRODUCTION_ID));

        when(armRpoDownloadProduction.downloadProduction(any(), any(), any()))
            .thenReturn(getFeignResponse(200));

        when(armRpoClient.removeProduction(any(), any()))
            .thenReturn(getRemoveProductionResponse());

        // when
        armRpoPollService.pollArmRpo(true);

        // then
        var updatedArmRpoExecutionDetailEntity = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetailEntity.getId());
        assertNotNull(updatedArmRpoExecutionDetailEntity);
        assertEquals(ArmRpoHelper.removeProductionRpoState().getId(), updatedArmRpoExecutionDetailEntity.get().getArmRpoState().getId());
        assertEquals(ArmRpoHelper.completedRpoStatus().getId(), updatedArmRpoExecutionDetailEntity.get().getArmRpoStatus().getId());

        verify(armRpoClient).getExtendedSearchesByMatter(any(), any());
        verify(armRpoClient).getMasterIndexFieldByRecordClassSchema(any(), any());
        verify(armRpoClient).createExportBasedOnSearchResultsTable(anyString(), any());
        verify(armRpoClient).getExtendedProductionsByMatter(anyString(), any());
        verify(armRpoClient).getProductionOutputFiles(any(), any());
        verify(armRpoDownloadProduction).downloadProduction(any(), any(), any());
        verify(armRpoClient).removeProduction(any(), any());
        verifyNoMoreInteractions(armRpoClient);

    }

    private @NotNull RemoveProductionResponse getRemoveProductionResponse() {
        RemoveProductionResponse response = new RemoveProductionResponse();
        response.setStatus(200);
        response.setIsError(false);
        return response;
    }

    private Response getFeignResponse(int status) throws IOException {

        Response.Body body = new Response.Body() {
            InputStream inputStream;

            @Override
            public Integer length() {
                return 124;
            }

            @Override
            public boolean isRepeatable() {
                return false;
            }

            @Override
            public InputStream asInputStream() throws IOException {
                File productionFile = TestUtils.getFile(PRODUCTIONEXPORTFILE_CSV);
                inputStream = Files.newInputStream(productionFile.toPath());
                return inputStream;
            }

            @Override
            public Reader asReader(Charset charset) throws IOException {
                return null;
            }

            @Override
            public void close() throws IOException {
                inputStream.close();
            }
        };

        Request request = Request.create(Request.HttpMethod.GET, "http://localhost:8080", Collections.emptyMap(), null,
                                         Charset.defaultCharset());
        return Response.builder()
            .status(status)
            .body(body)
            .request(request)
            .build();
        
    }

    private @NotNull ProductionOutputFilesResponse getProductionOutputFilesResponse(String fileId) {
        var productionExportFileDetail = new ProductionOutputFilesResponse.ProductionExportFileDetail();
        productionExportFileDetail.setProductionExportFileId(fileId);

        var productionExportFile = new ProductionOutputFilesResponse.ProductionExportFile();
        productionExportFile.setProductionExportFileDetails(productionExportFileDetail);

        var response = new ProductionOutputFilesResponse();
        response.setStatus(200);
        response.setIsError(false);
        response.setProductionExportFiles(Collections.singletonList(productionExportFile));

        return response;
    }

    private @NotNull ExtendedProductionsByMatterResponse getExtendedProductionsByMatterResponse() {
        ExtendedProductionsByMatterResponse response = new ExtendedProductionsByMatterResponse();
        response.setStatus(200);
        response.setIsError(false);
        ExtendedProductionsByMatterResponse.Productions productions = new ExtendedProductionsByMatterResponse.Productions();
        productions.setProductionId(PRODUCTION_ID);
        response.setProductions(List.of(productions));
        return response;
    }

    private @NotNull CreateExportBasedOnSearchResultsTableResponse getCreateExportBasedOnSearchResultsTableResponse() {
        CreateExportBasedOnSearchResultsTableResponse response = new CreateExportBasedOnSearchResultsTableResponse();
        response.setStatus(200);
        response.setIsError(false);
        response.setResponseStatus(2);
        return response;
    }

    private @NotNull CreateExportBasedOnSearchResultsTableResponse getCreateExportBasedOnSearchResultsTableResponseInProgress() {
        CreateExportBasedOnSearchResultsTableResponse response = new CreateExportBasedOnSearchResultsTableResponse();
        response.setStatus(400);
        response.setIsError(false);
        response.setResponseStatus(2);
        return response;
    }

    private @NotNull MasterIndexFieldByRecordClassSchemaResponse getMasterIndexFieldByRecordClassSchemaResponse(String propertyName1,
                                                                                                                String propertyName2) {
        MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField masterIndexField1 = new MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField();
        masterIndexField1.setMasterIndexFieldId("1");
        masterIndexField1.setDisplayName("displayName");
        masterIndexField1.setPropertyName(propertyName1);
        masterIndexField1.setPropertyType("propertyType");
        masterIndexField1.setIsMasked(true);

        MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField masterIndexField2 = new MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField();
        masterIndexField2.setMasterIndexFieldId("2");
        masterIndexField2.setDisplayName("displayName");
        masterIndexField2.setPropertyName(propertyName2);
        masterIndexField2.setPropertyType("propertyType");
        masterIndexField2.setIsMasked(false);

        MasterIndexFieldByRecordClassSchemaResponse response = new MasterIndexFieldByRecordClassSchemaResponse();
        response.setMasterIndexFields(List.of(masterIndexField1, masterIndexField2));
        return response;
    }

    private ExtendedSearchesByMatterResponse getExtendedSearchesByMatterResponse() {
        ExtendedSearchesByMatterResponse response = new ExtendedSearchesByMatterResponse();
        response.setStatus(200);
        response.setIsError(false);
        ExtendedSearchesByMatterResponse.Search search = new ExtendedSearchesByMatterResponse.Search();
        search.setTotalCount(4);
        ExtendedSearchesByMatterResponse.SearchDetail searchDetail = new ExtendedSearchesByMatterResponse.SearchDetail();
        searchDetail.setSearch(search);
        response.setSearches(List.of(searchDetail));
        return response;
    }
}
