package uk.gov.hmcts.darts.arm.service;

import feign.Request;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.CreateExportBasedOnSearchResultsTableResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.ExtendedProductionsByMatterResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.ExtendedSearchesByMatterResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.MasterIndexFieldByRecordClassSchemaResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.ProductionOutputFilesResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.RemoveProductionResponse;
import uk.gov.hmcts.darts.arm.component.ArmRpoDownloadProduction;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.service.impl.ArmApiServiceImpl;
import uk.gov.hmcts.darts.arm.service.impl.ArmRpoPollServiceImpl;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
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
import java.time.Duration;
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
import static uk.gov.hmcts.darts.arm.enums.ArmRpoResponseStatusCode.READY_STATUS;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getArmRpoExecutionDetailTestData;

@TestPropertySource(properties = {"darts.storage.arm.is-mock-arm-rpo-download-csv=false"})
@Slf4j
@SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.CloseResource"})
class ArmRpoPollServiceIntTest extends PostgresIntegrationBase {

    private static final String BEARER_TOKEN = "BearerToken";
    private static final String PRODUCTIONEXPORTFILE_CSV = "tests/arm/service/ArmRpoPollServiceTest/productionexportfile.csv";
    private static final String PRODUCTION_NAME = "DARTS_RPO_2024-08-13";
    private static final String PRODUCTION_ID = " b52268a3-75e5-4dd4-a8d3-0b43781cfcf9";
    private static final String SEARCH_ID = "8271f101-8c14-4c41-8865-edc5d8baed99";
    private static final String MATTER_ID = "cb70c7fa-8972-4400-af1d-ff5dd76d2104";
    private static final String STORAGE_ACCOUNT_ID = "StorageAccountId";
    private static final String PROPERTY_NAME = "propertyName";
    private static final String INGESTION_DATE = "ingestionDate";
    private static final int HTTP_STATUS_OK = 200;
    private static final int HTTP_STATUS_400 = 400;

    @MockitoBean
    private UserIdentity userIdentity;
    @MockitoBean
    private ArmRpoClient armRpoClient;
    @MockitoBean
    private ArmApiServiceImpl armApiService;
    @MockitoBean
    private ArmRpoDownloadProduction armRpoDownloadProduction;
    @MockitoBean
    private ArmRpoUtil armRpoUtil;

    @MockitoSpyBean
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @TempDir
    protected File tempDirectory;

    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity;
    private String uniqueProductionName;
    private final Duration pollDuration = Duration.ofHours(4);

    @Autowired
    private ArmRpoPollServiceImpl armRpoPollService;

    @BeforeEach
    void setUp() {

        UserAccountEntity userAccountEntity = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        lenient().when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        armRpoExecutionDetailEntity = dartsPersistence.save(getArmRpoExecutionDetailTestData().minimalArmRpoExecutionDetailEntity());

        String fileLocation = tempDirectory.getAbsolutePath();
        lenient().when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

        uniqueProductionName = PRODUCTION_NAME + "_UUID_CSV";
        lenient().when(armRpoUtil.generateUniqueProductionName(anyString())).thenReturn(uniqueProductionName);
    }

    @Test
    void pollArmRpo_shouldPollSuccessfully_WithSaveBackgroundCompleted() throws IOException {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ArmRpoHelper.completedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ArmRpoHelper.saveBackgroundSearchRpoState());
        armRpoExecutionDetailEntity.setMatterId(MATTER_ID);
        armRpoExecutionDetailEntity.setSearchId(SEARCH_ID);
        armRpoExecutionDetailEntity.setStorageAccountId(STORAGE_ACCOUNT_ID);
        armRpoExecutionDetailEntity.setProductionId(PRODUCTION_ID);
        armRpoExecutionDetailEntity = dartsPersistence.save(armRpoExecutionDetailEntity);

        when(armApiService.getArmBearerToken()).thenReturn(BEARER_TOKEN);
        when(armRpoClient.getExtendedSearchesByMatter(any(), any()))
            .thenReturn(getExtendedSearchesByMatterResponse());
        when(armRpoClient.getMasterIndexFieldByRecordClassSchema(any(), any()))
            .thenReturn(getMasterIndexFieldByRecordClassSchemaResponse(PROPERTY_NAME, INGESTION_DATE));
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any()))
            .thenReturn(getCreateExportBasedOnSearchResultsTableResponse());
        when(armRpoClient.getExtendedProductionsByMatter(anyString(), any()))
            .thenReturn(getExtendedProductionsByMatterResponse());
        when(armRpoClient.getProductionOutputFiles(any(), any()))
            .thenReturn(getProductionOutputFilesResponse(PRODUCTION_ID));

        when(armRpoDownloadProduction.downloadProduction(any(), any(), any()))
            .thenReturn(getFeignResponse(HTTP_STATUS_OK));

        when(armRpoClient.removeProduction(any(), any()))
            .thenReturn(getRemoveProductionResponse());

        // when
        armRpoPollService.pollArmRpo(false, pollDuration);

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
    void pollArmRpo_shouldPollSuccessfully_WithGetExtendedSearchesByMatterInProgress() throws IOException {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ArmRpoHelper.inProgressRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ArmRpoHelper.getExtendedSearchesByMatterRpoState());
        armRpoExecutionDetailEntity.setMatterId(MATTER_ID);
        armRpoExecutionDetailEntity.setSearchId(SEARCH_ID);
        armRpoExecutionDetailEntity.setStorageAccountId(STORAGE_ACCOUNT_ID);
        armRpoExecutionDetailEntity.setProductionId(PRODUCTION_ID);
        armRpoExecutionDetailEntity = dartsPersistence.save(armRpoExecutionDetailEntity);

        when(armApiService.getArmBearerToken()).thenReturn(BEARER_TOKEN);
        when(armRpoClient.getExtendedSearchesByMatter(any(), any()))
            .thenReturn(getExtendedSearchesByMatterResponse());
        when(armRpoClient.getMasterIndexFieldByRecordClassSchema(any(), any()))
            .thenReturn(getMasterIndexFieldByRecordClassSchemaResponse(PROPERTY_NAME, INGESTION_DATE));
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any()))
            .thenReturn(getCreateExportBasedOnSearchResultsTableResponse());
        when(armRpoClient.getExtendedProductionsByMatter(anyString(), any()))
            .thenReturn(getExtendedProductionsByMatterResponse());
        when(armRpoClient.getProductionOutputFiles(any(), any()))
            .thenReturn(getProductionOutputFilesResponse(PRODUCTION_ID));

        when(armRpoDownloadProduction.downloadProduction(any(), any(), any()))
            .thenReturn(getFeignResponse(HTTP_STATUS_OK));

        when(armRpoClient.removeProduction(any(), any()))
            .thenReturn(getRemoveProductionResponse());

        // when
        armRpoPollService.pollArmRpo(false, pollDuration);

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
    void pollArmRpo_shouldPollSuccessfully_WithSaveBackgroundCompletedAndFinishWithCreateExportInProgress() {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ArmRpoHelper.completedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ArmRpoHelper.saveBackgroundSearchRpoState());
        armRpoExecutionDetailEntity.setMatterId(MATTER_ID);
        armRpoExecutionDetailEntity.setSearchId(SEARCH_ID);
        armRpoExecutionDetailEntity.setStorageAccountId(STORAGE_ACCOUNT_ID);
        armRpoExecutionDetailEntity.setProductionId(PRODUCTION_ID);
        armRpoExecutionDetailEntity = dartsPersistence.save(armRpoExecutionDetailEntity);

        when(armApiService.getArmBearerToken()).thenReturn(BEARER_TOKEN);
        when(armRpoClient.getExtendedSearchesByMatter(any(), any()))
            .thenReturn(getExtendedSearchesByMatterResponse());
        when(armRpoClient.getMasterIndexFieldByRecordClassSchema(any(), any()))
            .thenReturn(getMasterIndexFieldByRecordClassSchemaResponse(PROPERTY_NAME, INGESTION_DATE));
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any()))
            .thenReturn(getCreateExportBasedOnSearchResultsTableResponseInProgress());

        // when
        armRpoPollService.pollArmRpo(false, pollDuration);

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
    void pollArmRpo_shouldPollSuccessfully_WithGetExtendedProductionsByMatterInProgress() throws IOException {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ArmRpoHelper.inProgressRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ArmRpoHelper.getExtendedSearchesByMatterRpoState());
        armRpoExecutionDetailEntity.setMatterId(MATTER_ID);
        armRpoExecutionDetailEntity.setSearchId(SEARCH_ID);
        armRpoExecutionDetailEntity.setStorageAccountId(STORAGE_ACCOUNT_ID);
        armRpoExecutionDetailEntity.setProductionId(PRODUCTION_ID);
        armRpoExecutionDetailEntity = dartsPersistence.save(armRpoExecutionDetailEntity);

        when(armApiService.getArmBearerToken()).thenReturn(BEARER_TOKEN);
        when(armRpoClient.getExtendedSearchesByMatter(any(), any()))
            .thenReturn(getExtendedSearchesByMatterResponse());
        when(armRpoClient.getMasterIndexFieldByRecordClassSchema(any(), any()))
            .thenReturn(getMasterIndexFieldByRecordClassSchemaResponse(PROPERTY_NAME, INGESTION_DATE));
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any()))
            .thenReturn(getCreateExportBasedOnSearchResultsTableResponse());
        when(armRpoClient.getExtendedProductionsByMatter(anyString(), any()))
            .thenReturn(getExtendedProductionsByMatterResponse());
        when(armRpoClient.getProductionOutputFiles(any(), any()))
            .thenReturn(getProductionOutputFilesResponse(PRODUCTION_ID));

        when(armRpoDownloadProduction.downloadProduction(any(), any(), any()))
            .thenReturn(getFeignResponse(HTTP_STATUS_OK));

        when(armRpoClient.removeProduction(any(), any()))
            .thenReturn(getRemoveProductionResponse());

        // when
        armRpoPollService.pollArmRpo(false, pollDuration);

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
    void pollArmRpo_shouldPollSuccessfully_WithGetProductionOutputFilesInProgress() throws IOException {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ArmRpoHelper.inProgressRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ArmRpoHelper.getProductionOutputFilesRpoState());
        armRpoExecutionDetailEntity.setMatterId(MATTER_ID);
        armRpoExecutionDetailEntity.setSearchId(SEARCH_ID);
        armRpoExecutionDetailEntity.setStorageAccountId(STORAGE_ACCOUNT_ID);
        armRpoExecutionDetailEntity.setProductionId(PRODUCTION_ID);
        armRpoExecutionDetailEntity = dartsPersistence.save(armRpoExecutionDetailEntity);

        when(armApiService.getArmBearerToken()).thenReturn(BEARER_TOKEN);
        when(armRpoClient.getExtendedSearchesByMatter(any(), any()))
            .thenReturn(getExtendedSearchesByMatterResponse());
        when(armRpoClient.getMasterIndexFieldByRecordClassSchema(any(), any()))
            .thenReturn(getMasterIndexFieldByRecordClassSchemaResponse(PROPERTY_NAME, INGESTION_DATE));
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any()))
            .thenReturn(getCreateExportBasedOnSearchResultsTableResponse());
        when(armRpoClient.getExtendedProductionsByMatter(anyString(), any()))
            .thenReturn(getExtendedProductionsByMatterResponse());
        when(armRpoClient.getProductionOutputFiles(any(), any()))
            .thenReturn(getProductionOutputFilesResponse(PRODUCTION_ID));

        when(armRpoDownloadProduction.downloadProduction(any(), any(), any()))
            .thenReturn(getFeignResponse(HTTP_STATUS_OK));

        when(armRpoClient.removeProduction(any(), any()))
            .thenReturn(getRemoveProductionResponse());

        // when
        armRpoPollService.pollArmRpo(false, pollDuration);

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
    void pollArmRpo_shouldPollSuccessfully_WithFailedDownloadProductionIsManual() throws IOException {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ArmRpoHelper.failedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ArmRpoHelper.downloadProductionRpoState());
        armRpoExecutionDetailEntity.setMatterId(MATTER_ID);
        armRpoExecutionDetailEntity.setSearchId(SEARCH_ID);
        armRpoExecutionDetailEntity.setStorageAccountId(STORAGE_ACCOUNT_ID);
        armRpoExecutionDetailEntity.setProductionId(PRODUCTION_ID);
        armRpoExecutionDetailEntity = dartsPersistence.save(armRpoExecutionDetailEntity);

        when(armApiService.getArmBearerToken()).thenReturn(BEARER_TOKEN);
        when(armRpoClient.getExtendedSearchesByMatter(any(), any()))
            .thenReturn(getExtendedSearchesByMatterResponse());
        when(armRpoClient.getMasterIndexFieldByRecordClassSchema(any(), any()))
            .thenReturn(getMasterIndexFieldByRecordClassSchemaResponse(PROPERTY_NAME, INGESTION_DATE));
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any()))
            .thenReturn(getCreateExportBasedOnSearchResultsTableResponse());
        when(armRpoClient.getExtendedProductionsByMatter(anyString(), any()))
            .thenReturn(getExtendedProductionsByMatterResponse());
        when(armRpoClient.getProductionOutputFiles(any(), any()))
            .thenReturn(getProductionOutputFilesResponse(PRODUCTION_ID));

        when(armRpoDownloadProduction.downloadProduction(any(), any(), any()))
            .thenReturn(getFeignResponse(HTTP_STATUS_OK));

        when(armRpoClient.removeProduction(any(), any()))
            .thenReturn(getRemoveProductionResponse());

        // when
        armRpoPollService.pollArmRpo(true, pollDuration);

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
        response.setStatus(HTTP_STATUS_OK);
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
            public Reader asReader(Charset charset) {
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
        productionExportFileDetail.setStatus(READY_STATUS.getStatusCode());

        var productionExportFile = new ProductionOutputFilesResponse.ProductionExportFile();
        productionExportFile.setProductionExportFileDetails(productionExportFileDetail);

        var response = new ProductionOutputFilesResponse();
        response.setStatus(HTTP_STATUS_OK);
        response.setIsError(false);
        response.setProductionExportFiles(Collections.singletonList(productionExportFile));

        return response;
    }

    private @NotNull ExtendedProductionsByMatterResponse getExtendedProductionsByMatterResponse() {
        ExtendedProductionsByMatterResponse response = new ExtendedProductionsByMatterResponse();
        response.setStatus(HTTP_STATUS_OK);
        response.setIsError(false);
        ExtendedProductionsByMatterResponse.Productions productions = new ExtendedProductionsByMatterResponse.Productions();
        productions.setProductionId(PRODUCTION_ID);
        productions.setName(uniqueProductionName);
        productions.setEndProductionTime("2025-01-16T12:30:09.9129726+00:00");
        response.setProductions(List.of(productions));
        return response;
    }

    private @NotNull CreateExportBasedOnSearchResultsTableResponse getCreateExportBasedOnSearchResultsTableResponse() {
        CreateExportBasedOnSearchResultsTableResponse response = new CreateExportBasedOnSearchResultsTableResponse();
        response.setStatus(HTTP_STATUS_OK);
        response.setIsError(false);
        response.setResponseStatus(2);
        return response;
    }

    private @NotNull CreateExportBasedOnSearchResultsTableResponse getCreateExportBasedOnSearchResultsTableResponseInProgress() {
        CreateExportBasedOnSearchResultsTableResponse response = new CreateExportBasedOnSearchResultsTableResponse();
        response.setStatus(HTTP_STATUS_400);
        response.setIsError(false);
        response.setResponseStatus(2);
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

    private ExtendedSearchesByMatterResponse getExtendedSearchesByMatterResponse() {
        ExtendedSearchesByMatterResponse response = new ExtendedSearchesByMatterResponse();
        response.setStatus(HTTP_STATUS_OK);
        response.setIsError(false);
        ExtendedSearchesByMatterResponse.Search search = new ExtendedSearchesByMatterResponse.Search();
        search.setTotalCount(4);
        search.setName(PRODUCTION_NAME);
        search.setIsSaved(true);
        search.setSearchId(SEARCH_ID);
        ExtendedSearchesByMatterResponse.SearchDetail searchDetail = new ExtendedSearchesByMatterResponse.SearchDetail();
        searchDetail.setSearch(search);
        response.setSearches(List.of(searchDetail));
        return response;
    }
}
