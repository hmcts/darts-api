package uk.gov.hmcts.darts.arm.service.impl;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.exception.ArmRpoGetExtendedSearchesByMatterIdException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelperMocks;
import uk.gov.hmcts.darts.arm.model.rpo.MasterIndexFieldByRecordClassSchema;
import uk.gov.hmcts.darts.arm.rpo.ArmRpoApi;
import uk.gov.hmcts.darts.arm.service.ArmApiService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.CloseResource")
class ArmRpoPollServiceImplTest {

    private static final String PRODUCTION_NAME = "DARTS_RPO_2024-08-13";

    @Mock
    private ArmRpoApi armRpoApi;
    @Mock
    private ArmApiService armApiService;
    @Mock
    private ArmRpoService armRpoService;
    @Mock
    private UserIdentity userIdentity;
    @Mock
    private FileOperationService fileOperationService;
    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @Mock
    private LogApi logApi;

    @Mock
    private UserAccountEntity userAccountEntity;

    @TempDir
    private File tempDirectory;

    private final List<File> tempProductionFiles = new ArrayList<>();
    private final List<Integer> allowableFailedStates = new ArrayList<>();

    private static final Integer EXECUTION_ID = 1;
    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity;
    private static final ArmRpoHelperMocks ARM_RPO_HELPER_MOCKS = new ArmRpoHelperMocks();

    private ArmRpoPollServiceImpl armRpoPollService;


    @BeforeEach
    void setUp() {
        armRpoPollService = new ArmRpoPollServiceImpl(armRpoApi, armApiService, armRpoService, userIdentity, fileOperationService,
                                                      armDataManagementConfiguration, logApi, tempProductionFiles, allowableFailedStates);

        lenient().when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(EXECUTION_ID);
        when(armRpoService.getLatestArmRpoExecutionDetailEntity()).thenReturn(armRpoExecutionDetailEntity);
    }

    @Test
    void pollArmRpo_shouldPollSuccessfully_whenSaveBackgroundCompleted() throws IOException {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getSaveBackgroundSearchRpoState());

        when(armApiService.getArmBearerToken()).thenReturn("bearerToken");
        when(armRpoApi.getExtendedSearchesByMatter(anyString(), anyInt(), any())).thenReturn(PRODUCTION_NAME);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();
        when(armRpoApi.getMasterIndexFieldByRecordClassSchema(anyString(), anyInt(), any(), any()))
            .thenReturn(headerColumns);
        when(armRpoApi.createExportBasedOnSearchResultsTable(anyString(), anyInt(), any(), any(), any())).thenReturn(true);
        when(armRpoApi.getProductionOutputFiles(anyString(), anyInt(), any())).thenReturn(List.of("fileId"));
        InputStream resource = IOUtils.toInputStream("dummy input stream", "UTF-8");
        when(armRpoApi.downloadProduction(anyString(), anyInt(), anyString(), any())).thenReturn(resource);
        doNothing().when(armRpoApi).removeProduction(anyString(), anyInt(), any());

        String fileName = "fileId.csv";
        Path filePath = Path.of(tempDirectory.getAbsolutePath()).resolve(fileName);
        when(fileOperationService.saveFileToTempWorkspace(any(InputStream.class), anyString(), any(), anyBoolean())).thenReturn(filePath);

        // when
        armRpoPollService.pollArmRpo(false);

        // then
        verify(armRpoApi).getExtendedSearchesByMatter("bearerToken", 1, userAccountEntity);
        verify(armRpoApi).getMasterIndexFieldByRecordClassSchema("bearerToken", 1, ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState(),
                                                                 userAccountEntity);
        verify(armRpoApi).createExportBasedOnSearchResultsTable("bearerToken", 1, headerColumns, PRODUCTION_NAME, userAccountEntity);
        verify(armRpoApi).getExtendedProductionsByMatter("bearerToken", 1, userAccountEntity);
        verify(armRpoApi).getProductionOutputFiles("bearerToken", 1, userAccountEntity);
        verify(armRpoApi).downloadProduction("bearerToken", 1, "fileId", userAccountEntity);
        verify(armRpoApi).removeProduction("bearerToken", 1, userAccountEntity);

        verify(userIdentity).getUserAccount();

        verify(fileOperationService).saveFileToTempWorkspace(resource, "productionExportFileId_fileId.csv", armDataManagementConfiguration, true);

        verify(logApi).armRpoPollingSuccessful(EXECUTION_ID);

        verifyNoMoreInteractions(armRpoApi, userIdentity, fileOperationService, logApi);
    }

    @Test
    void pollArmRpo_shouldPollSuccessfully_whenSaveBackgroundCompletedForManualRun() throws IOException {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getSaveBackgroundSearchRpoState());

        when(armApiService.getArmBearerToken()).thenReturn("bearerToken");
        when(armRpoApi.getExtendedSearchesByMatter(anyString(), anyInt(), any())).thenReturn(PRODUCTION_NAME);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();
        when(armRpoApi.getMasterIndexFieldByRecordClassSchema(anyString(), anyInt(), any(), any()))
            .thenReturn(headerColumns);
        when(armRpoApi.createExportBasedOnSearchResultsTable(anyString(), anyInt(), any(), any(), any())).thenReturn(true);
        when(armRpoApi.getProductionOutputFiles(anyString(), anyInt(), any())).thenReturn(List.of("fileId"));
        InputStream resource = IOUtils.toInputStream("dummy input stream", "UTF-8");
        when(armRpoApi.downloadProduction(anyString(), anyInt(), anyString(), any())).thenReturn(resource);
        doNothing().when(armRpoApi).removeProduction(anyString(), anyInt(), any());

        String fileName = "fileId.csv";
        Path filePath = Path.of(tempDirectory.getAbsolutePath()).resolve(fileName);
        when(fileOperationService.saveFileToTempWorkspace(any(InputStream.class), anyString(), any(), anyBoolean())).thenReturn(filePath);

        // when
        armRpoPollService.pollArmRpo(true);

        // then
        verify(armRpoApi).getExtendedSearchesByMatter("bearerToken", 1, userAccountEntity);
        verify(armRpoApi).getMasterIndexFieldByRecordClassSchema("bearerToken", 1,
                                                                 ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState(), userAccountEntity);
        verify(armRpoApi).createExportBasedOnSearchResultsTable("bearerToken", 1, headerColumns, PRODUCTION_NAME, userAccountEntity);
        verify(armRpoApi).getExtendedProductionsByMatter("bearerToken", 1, userAccountEntity);
        verify(armRpoApi).getProductionOutputFiles("bearerToken", 1, userAccountEntity);
        verify(armRpoApi).downloadProduction("bearerToken", 1, "fileId", userAccountEntity);
        verify(armRpoApi).removeProduction("bearerToken", 1, userAccountEntity);

        verify(userIdentity).getUserAccount();

        verify(fileOperationService).saveFileToTempWorkspace(resource, "productionExportFileId_fileId.csv", armDataManagementConfiguration, true);

        verify(logApi).armRpoPollingSuccessful(EXECUTION_ID);

        verifyNoMoreInteractions(armRpoApi, userIdentity, fileOperationService, logApi);
    }

    @Test
    void pollArmRpo_shouldPollSuccessfully_whenGetExtendedSearchesByMatterInProgress() throws IOException {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getGetExtendedSearchesByMatterRpoState());

        when(armApiService.getArmBearerToken()).thenReturn("bearerToken");
        when(armRpoApi.getExtendedSearchesByMatter(anyString(), anyInt(), any())).thenReturn(PRODUCTION_NAME);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();
        when(armRpoApi.getMasterIndexFieldByRecordClassSchema(anyString(), anyInt(), any(), any()))
            .thenReturn(headerColumns);
        when(armRpoApi.createExportBasedOnSearchResultsTable(anyString(), anyInt(), any(), any(), any())).thenReturn(true);
        when(armRpoApi.getProductionOutputFiles(anyString(), anyInt(), any())).thenReturn(List.of("fileId"));
        InputStream resource = IOUtils.toInputStream("dummy input stream", "UTF-8");
        when(armRpoApi.downloadProduction(anyString(), anyInt(), anyString(), any())).thenReturn(resource);
        doNothing().when(armRpoApi).removeProduction(anyString(), anyInt(), any());

        String fileName = "fileId.csv";
        Path filePath = Path.of(tempDirectory.getAbsolutePath()).resolve(fileName);
        when(fileOperationService.saveFileToTempWorkspace(any(InputStream.class), anyString(), any(), anyBoolean())).thenReturn(filePath);

        // when
        armRpoPollService.pollArmRpo(false);

        // then
        verify(armRpoApi).getExtendedSearchesByMatter("bearerToken", 1, userAccountEntity);
        verify(armRpoApi).getMasterIndexFieldByRecordClassSchema("bearerToken", 1, ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState(),
                                                                 userAccountEntity);
        verify(armRpoApi).createExportBasedOnSearchResultsTable("bearerToken", 1, headerColumns, PRODUCTION_NAME, userAccountEntity);
        verify(armRpoApi).getExtendedProductionsByMatter("bearerToken", 1, userAccountEntity);
        verify(armRpoApi).getProductionOutputFiles("bearerToken", 1, userAccountEntity);
        verify(armRpoApi).downloadProduction("bearerToken", 1, "fileId", userAccountEntity);
        verify(armRpoApi).removeProduction("bearerToken", 1, userAccountEntity);

        verify(userIdentity).getUserAccount();

        verify(fileOperationService).saveFileToTempWorkspace(resource, "productionExportFileId_fileId.csv", armDataManagementConfiguration, true);

        verify(logApi).armRpoPollingSuccessful(EXECUTION_ID);

        verifyNoMoreInteractions(armRpoApi, userIdentity, fileOperationService, logApi);
    }

    @Test
    void pollArmRpo_shouldPollSuccessfully_whenCreateExportBasedOnSearchResultsTableInProgress() throws IOException {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getCreateExportBasedOnSearchResultsTableRpoState());

        when(armApiService.getArmBearerToken()).thenReturn("bearerToken");
        when(armRpoApi.getExtendedSearchesByMatter(anyString(), anyInt(), any())).thenReturn(PRODUCTION_NAME);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();
        when(armRpoApi.getMasterIndexFieldByRecordClassSchema(anyString(), anyInt(), any(), any()))
            .thenReturn(headerColumns);
        when(armRpoApi.createExportBasedOnSearchResultsTable(anyString(), anyInt(), any(), any(), any())).thenReturn(true);
        when(armRpoApi.getProductionOutputFiles(anyString(), anyInt(), any())).thenReturn(List.of("fileId"));
        InputStream resource = IOUtils.toInputStream("dummy input stream", "UTF-8");
        when(armRpoApi.downloadProduction(anyString(), anyInt(), anyString(), any())).thenReturn(resource);
        doNothing().when(armRpoApi).removeProduction(anyString(), anyInt(), any());

        String fileName = "fileId.csv";
        Path filePath = Path.of(tempDirectory.getAbsolutePath()).resolve(fileName);
        when(fileOperationService.saveFileToTempWorkspace(any(InputStream.class), anyString(), any(), anyBoolean())).thenReturn(filePath);

        // when
        armRpoPollService.pollArmRpo(false);

        // then
        verify(armRpoApi).getExtendedSearchesByMatter("bearerToken", 1, userAccountEntity);
        verify(armRpoApi).getMasterIndexFieldByRecordClassSchema("bearerToken", 1, ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState(),
                                                                 userAccountEntity);
        verify(armRpoApi).createExportBasedOnSearchResultsTable("bearerToken", 1, headerColumns, PRODUCTION_NAME, userAccountEntity);
        verify(armRpoApi).getExtendedProductionsByMatter("bearerToken", 1, userAccountEntity);
        verify(armRpoApi).getProductionOutputFiles("bearerToken", 1, userAccountEntity);
        verify(armRpoApi).downloadProduction("bearerToken", 1, "fileId", userAccountEntity);
        verify(armRpoApi).removeProduction("bearerToken", 1, userAccountEntity);

        verify(userIdentity).getUserAccount();

        verify(fileOperationService).saveFileToTempWorkspace(resource, "productionExportFileId_fileId.csv", armDataManagementConfiguration, true);

        verify(logApi).armRpoPollingSuccessful(EXECUTION_ID);

        verifyNoMoreInteractions(armRpoApi, userIdentity, fileOperationService, logApi);
    }

    @Test
    void pollArmRpo_shouldPollSuccessfully_whenCreateExportBasedOnSearchResultsTableForManualRun() throws IOException {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getCreateExportBasedOnSearchResultsTableRpoState());

        when(armApiService.getArmBearerToken()).thenReturn("bearerToken");
        when(armRpoApi.getExtendedSearchesByMatter(anyString(), anyInt(), any())).thenReturn(PRODUCTION_NAME);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();
        when(armRpoApi.getMasterIndexFieldByRecordClassSchema(anyString(), anyInt(), any(), any()))
            .thenReturn(headerColumns);
        when(armRpoApi.createExportBasedOnSearchResultsTable(anyString(), anyInt(), any(), any(), any())).thenReturn(true);
        when(armRpoApi.getProductionOutputFiles(anyString(), anyInt(), any())).thenReturn(List.of("fileId"));
        InputStream resource = IOUtils.toInputStream("dummy input stream", "UTF-8");
        when(armRpoApi.downloadProduction(anyString(), anyInt(), anyString(), any())).thenReturn(resource);
        doNothing().when(armRpoApi).removeProduction(anyString(), anyInt(), any());

        String fileName = "fileId.csv";
        Path filePath = Path.of(tempDirectory.getAbsolutePath()).resolve(fileName);
        when(fileOperationService.saveFileToTempWorkspace(any(InputStream.class), anyString(), any(), anyBoolean())).thenReturn(filePath);

        // when
        armRpoPollService.pollArmRpo(true);

        // then
        verify(armRpoApi).getExtendedSearchesByMatter("bearerToken", 1, userAccountEntity);
        verify(armRpoApi).getMasterIndexFieldByRecordClassSchema("bearerToken", 1, ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState(),
                                                                 userAccountEntity);
        verify(armRpoApi).createExportBasedOnSearchResultsTable("bearerToken", 1, headerColumns, PRODUCTION_NAME, userAccountEntity);
        verify(armRpoApi).getExtendedProductionsByMatter("bearerToken", 1, userAccountEntity);
        verify(armRpoApi).getProductionOutputFiles("bearerToken", 1, userAccountEntity);
        verify(armRpoApi).downloadProduction("bearerToken", 1, "fileId", userAccountEntity);
        verify(armRpoApi).removeProduction("bearerToken", 1, userAccountEntity);

        verify(userIdentity).getUserAccount();

        verify(fileOperationService).saveFileToTempWorkspace(resource, "productionExportFileId_fileId.csv", armDataManagementConfiguration, true);

        verify(logApi).armRpoPollingSuccessful(any());

        verifyNoMoreInteractions(armRpoApi, userIdentity, fileOperationService, logApi);
    }

    @Test
    void pollArmRpo_shouldPollSuccessfully_whenDownloadProductionFailedOnPreviousAttemptForManualRun() throws IOException {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getDownloadProductionRpoState());

        when(armApiService.getArmBearerToken()).thenReturn("bearerToken");
        when(armRpoApi.getExtendedSearchesByMatter(anyString(), anyInt(), any())).thenReturn(PRODUCTION_NAME);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();
        when(armRpoApi.getMasterIndexFieldByRecordClassSchema(anyString(), anyInt(), any(), any()))
            .thenReturn(headerColumns);
        when(armRpoApi.createExportBasedOnSearchResultsTable(anyString(), anyInt(), any(), any(), any())).thenReturn(true);
        when(armRpoApi.getProductionOutputFiles(anyString(), anyInt(), any())).thenReturn(List.of("fileId"));
        when(armRpoApi.createExportBasedOnSearchResultsTable(anyString(), anyInt(), any(), any(), any())).thenReturn(true);
        when(armRpoApi.getProductionOutputFiles(anyString(), anyInt(), any())).thenReturn(List.of("fileId"));
        InputStream resource = IOUtils.toInputStream("dummy input stream", "UTF-8");
        when(armRpoApi.downloadProduction(anyString(), anyInt(), anyString(), any())).thenReturn(resource);
        doNothing().when(armRpoApi).removeProduction(anyString(), anyInt(), any());

        String fileName = "fileId.csv";
        Path filePath = Path.of(tempDirectory.getAbsolutePath()).resolve(fileName);
        when(fileOperationService.saveFileToTempWorkspace(any(InputStream.class), anyString(), any(), anyBoolean())).thenReturn(filePath);

        // when
        armRpoPollService.pollArmRpo(true);

        // then
        verify(armRpoApi).getExtendedSearchesByMatter("bearerToken", 1, userAccountEntity);
        verify(armRpoApi).getMasterIndexFieldByRecordClassSchema("bearerToken", 1, ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState(),
                                                                 userAccountEntity);
        verify(armRpoApi).createExportBasedOnSearchResultsTable("bearerToken", 1, headerColumns, PRODUCTION_NAME, userAccountEntity);
        verify(armRpoApi).getExtendedProductionsByMatter("bearerToken", 1, userAccountEntity);
        verify(armRpoApi).getProductionOutputFiles("bearerToken", 1, userAccountEntity);
        verify(armRpoApi).downloadProduction("bearerToken", 1, "fileId", userAccountEntity);
        verify(armRpoApi).removeProduction("bearerToken", 1, userAccountEntity);

        verify(userIdentity).getUserAccount();

        verify(fileOperationService).saveFileToTempWorkspace(resource, "productionExportFileId_fileId.csv", armDataManagementConfiguration, true);

        verify(logApi).armRpoPollingSuccessful(EXECUTION_ID);

        verifyNoMoreInteractions(armRpoApi, userIdentity, fileOperationService, logApi);
    }

    @Test
    void pollArmRpo_shouldPollNotFindLatestExecutionDetail_OnStepDownloadProductionFailed() {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getDownloadProductionRpoState());

        // when
        armRpoPollService.pollArmRpo(false);

        // then
        verify(armRpoService).getLatestArmRpoExecutionDetailEntity();

        verifyNoMoreInteractions(armRpoApi, armApiService, userIdentity, fileOperationService, logApi);
    }

    @Test
    void pollArmRpo_shouldPollNotFindLatestExecutionDetailForManualRun_OnStepDownloadProductionInProgress() {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getDownloadProductionRpoState());

        // when
        armRpoPollService.pollArmRpo(true);

        // then
        verify(armRpoService).getLatestArmRpoExecutionDetailEntity();

        verifyNoMoreInteractions(armRpoApi, armApiService, userIdentity, fileOperationService, logApi);
    }

    @Test
    void pollArmRpo_shouldHandleNoExecutionDetailEntity() {
        // given
        when(armRpoService.getLatestArmRpoExecutionDetailEntity()).thenReturn(null);

        // when
        armRpoPollService.pollArmRpo(false);

        // then
        verify(armRpoService).getLatestArmRpoExecutionDetailEntity();

        verifyNoMoreInteractions(armRpoApi, armApiService, userIdentity, fileOperationService, logApi);
    }

    @Test
    void pollArmRpo_shouldHandleNoBearerToken() {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getSaveBackgroundSearchRpoState());
        when(armRpoService.getLatestArmRpoExecutionDetailEntity()).thenReturn(armRpoExecutionDetailEntity);
        when(armApiService.getArmBearerToken()).thenReturn(null);

        // when
        armRpoPollService.pollArmRpo(false);

        // then
        verify(armRpoService).getLatestArmRpoExecutionDetailEntity();
        verify(armApiService).getArmBearerToken();
        verify(logApi).armRpoPollingFailed(EXECUTION_ID);
        verifyNoMoreInteractions(armRpoApi, userIdentity, fileOperationService, logApi);
    }

    @Test
    void pollArmRpo_shouldHandleCreateExportInProgress() {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getSaveBackgroundSearchRpoState());
        when(armApiService.getArmBearerToken()).thenReturn("bearerToken");
        when(armRpoApi.getExtendedSearchesByMatter(anyString(), anyInt(), any())).thenReturn(PRODUCTION_NAME);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();
        when(armRpoApi.getMasterIndexFieldByRecordClassSchema(anyString(), anyInt(), any(), any()))
            .thenReturn(headerColumns);
        when(armRpoApi.createExportBasedOnSearchResultsTable(anyString(), anyInt(), any(), any(), any())).thenReturn(false);

        // when
        armRpoPollService.pollArmRpo(false);

        // then
        verify(armRpoService).getLatestArmRpoExecutionDetailEntity();
        verify(armApiService).getArmBearerToken();
        verify(armRpoApi).getExtendedSearchesByMatter("bearerToken", 1, userAccountEntity);
        verify(armRpoApi).getMasterIndexFieldByRecordClassSchema("bearerToken", 1, ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState(),
                                                                 userAccountEntity);
        verify(armRpoApi).createExportBasedOnSearchResultsTable("bearerToken", 1, headerColumns, PRODUCTION_NAME, userAccountEntity);
        verify(userIdentity).getUserAccount();

        verifyNoMoreInteractions(armRpoApi, userIdentity, fileOperationService, logApi);
    }
    
    @Test
    void pollArmRpo_shouldHandleGetExtendedSearchesByMatterGetInProgress() {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getSaveBackgroundSearchRpoState());
        when(armApiService.getArmBearerToken()).thenReturn("bearerToken");
        when(armRpoApi.getExtendedSearchesByMatter(anyString(), anyInt(), any())).thenThrow(ArmRpoGetExtendedSearchesByMatterIdException.class);

        // when
        armRpoPollService.pollArmRpo(false);

        // then
        verify(armRpoService).getLatestArmRpoExecutionDetailEntity();
        verify(armApiService).getArmBearerToken();
        verify(armRpoApi).getExtendedSearchesByMatter("bearerToken", 1, userAccountEntity);
        verify(userIdentity).getUserAccount();

        verifyNoMoreInteractions(armRpoApi, userIdentity, fileOperationService, logApi);
    }

    @Test
    void pollArmRpo_shouldHandleNoProductionFiles() {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getSaveBackgroundSearchRpoState());
        when(armApiService.getArmBearerToken()).thenReturn("bearerToken");
        when(armRpoApi.getExtendedSearchesByMatter(anyString(), anyInt(), any())).thenReturn(PRODUCTION_NAME);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();
        when(armRpoApi.getMasterIndexFieldByRecordClassSchema(anyString(), anyInt(), any(), any()))
            .thenReturn(headerColumns);
        when(armRpoApi.createExportBasedOnSearchResultsTable(anyString(), anyInt(), any(), any(), any())).thenReturn(true);
        when(armRpoApi.getProductionOutputFiles(anyString(), anyInt(), any())).thenReturn(List.of());

        // when
        armRpoPollService.pollArmRpo(false);

        // then
        verify(armRpoService).getLatestArmRpoExecutionDetailEntity();
        verify(armApiService).getArmBearerToken();
        verify(armRpoApi).getExtendedSearchesByMatter("bearerToken", 1, userAccountEntity);
        verify(armRpoApi).getMasterIndexFieldByRecordClassSchema("bearerToken", 1, ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState(),
                                                                 userAccountEntity);
        verify(armRpoApi).createExportBasedOnSearchResultsTable("bearerToken", 1, headerColumns, PRODUCTION_NAME, userAccountEntity);
        verify(armRpoApi).getExtendedProductionsByMatter("bearerToken", 1, userAccountEntity);
        verify(armRpoApi).getProductionOutputFiles("bearerToken", 1, userAccountEntity);
        verify(userIdentity).getUserAccount();
        verify(logApi).armRpoPollingSuccessful(EXECUTION_ID);

        verifyNoMoreInteractions(armRpoApi, userIdentity, fileOperationService, logApi);
    }

    @Test
    void pollArmRpo_shouldHandleExceptionDuringPolling_whenCreateExportBasedOnSearchResultsTableStep() {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getSaveBackgroundSearchRpoState());
        when(armApiService.getArmBearerToken()).thenReturn("bearerToken");
        when(armRpoApi.getExtendedSearchesByMatter(anyString(), anyInt(), any())).thenReturn(PRODUCTION_NAME);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();
        when(armRpoApi.getMasterIndexFieldByRecordClassSchema(anyString(), anyInt(), any(), any()))
            .thenReturn(headerColumns);
        when(armRpoApi.createExportBasedOnSearchResultsTable(anyString(), anyInt(), any(), any(), any())).thenThrow(new ArmRpoException("Test exception"));

        // when
        armRpoPollService.pollArmRpo(false);

        // then
        verify(armRpoService).getLatestArmRpoExecutionDetailEntity();
        verify(armApiService).getArmBearerToken();
        verify(armRpoApi).getExtendedSearchesByMatter("bearerToken", 1, userAccountEntity);
        verify(armRpoApi).getMasterIndexFieldByRecordClassSchema("bearerToken", 1, ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState(),
                                                                 userAccountEntity);
        verify(armRpoApi).createExportBasedOnSearchResultsTable("bearerToken", 1, headerColumns, PRODUCTION_NAME, userAccountEntity);
        verify(userIdentity).getUserAccount();
        verify(logApi).armRpoPollingFailed(EXECUTION_ID);

        verifyNoMoreInteractions(armRpoApi, userIdentity, fileOperationService, logApi);
    }

    private List<MasterIndexFieldByRecordClassSchema> createHeaderColumns() {
        return List.of(
            createMasterIndexFieldByRecordClassSchema("200b9c27-b497-4977-82e7-1586b32a5871", "Record Class", "record_class", "string", false),
            createMasterIndexFieldByRecordClassSchema("90ee0e13-8639-4c4a-b542-66b6c8911549", "Archived Date", "ingestionDate", "date", false),
            createMasterIndexFieldByRecordClassSchema("a9b8daf2-d9ff-4815-b65a-f6ae2763b92c", "Client Identifier", "client_identifier", "string",
                                                      false),
            createMasterIndexFieldByRecordClassSchema("109b6bf1-57a0-48ec-b22e-c7248dc74f91", "Contributor", "contributor", "string", false),
            createMasterIndexFieldByRecordClassSchema("893048bf-1e7c-4811-9abf-00cd77a715cf", "Record Date", "recordDate", "date", false),
            createMasterIndexFieldByRecordClassSchema("fdd0fcbb-da46-4af1-a627-ac255c12bb23", "ObjectId", "bf_012", "number", false),
            createMasterIndexFieldByRecordClassSchema("1fa3bf91-5234-432d-bebb-b339dc3aaccf", "Region", "bf_012", "number", true)
        );
    }

    private MasterIndexFieldByRecordClassSchema createMasterIndexFieldByRecordClassSchema(
        String uuid, String displayName, String propertyName, String propertyType, boolean isMasked) {

        return MasterIndexFieldByRecordClassSchema.builder()
            .masterIndexField(uuid)
            .displayName(displayName)
            .propertyName(propertyName)
            .propertyType(propertyType)
            .isMasked(isMasked)
            .build();
    }

    @AfterAll
    static void close() {
        ARM_RPO_HELPER_MOCKS.close();
    }

}