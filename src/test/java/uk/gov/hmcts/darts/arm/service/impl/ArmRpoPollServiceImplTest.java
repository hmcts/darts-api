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
import uk.gov.hmcts.darts.arm.exception.ArmRpoInProgressException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelperMocks;
import uk.gov.hmcts.darts.arm.model.rpo.MasterIndexFieldByRecordClassSchema;
import uk.gov.hmcts.darts.arm.rpo.CreateExportBasedOnSearchResultsTableService;
import uk.gov.hmcts.darts.arm.rpo.DownloadProductionService;
import uk.gov.hmcts.darts.arm.rpo.GetExtendedProductionsByMatterService;
import uk.gov.hmcts.darts.arm.rpo.GetExtendedSearchesByMatterService;
import uk.gov.hmcts.darts.arm.rpo.GetMasterIndexFieldByRecordClassSchemaService;
import uk.gov.hmcts.darts.arm.rpo.GetProductionOutputFilesService;
import uk.gov.hmcts.darts.arm.rpo.RemoveProductionService;
import uk.gov.hmcts.darts.arm.service.ArmApiService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStateEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.CloseResource")
class ArmRpoPollServiceImplTest {

    private static final String PRODUCTION_NAME = "DARTS_RPO_2024-08-13";
    private static final int BATCH_SIZE = 10;

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
    private ArmRpoUtil armRpoUtil;
    @Mock
    private GetExtendedSearchesByMatterService getExtendedSearchesByMatterService;
    @Mock
    private GetMasterIndexFieldByRecordClassSchemaService getMasterIndexFieldByRecordClassSchemaService;
    @Mock
    private CreateExportBasedOnSearchResultsTableService createExportBasedOnSearchResultsTableService;
    @Mock
    private GetExtendedProductionsByMatterService getExtendedProductionsByMatterService;
    @Mock
    private GetProductionOutputFilesService getProductionOutputFilesService;
    @Mock
    private DownloadProductionService downloadProductionService;
    @Mock
    private RemoveProductionService removeProductionService;

    @Mock
    private UserAccountEntity userAccountEntity;

    @TempDir
    private File tempDirectory;

    private final List<File> tempProductionFiles = new ArrayList<>();
    private final List<Integer> allowableFailedStates = new ArrayList<>();
    private final List<Integer> inProgressStates = new ArrayList<>();

    private static final Integer EXECUTION_ID = 1;
    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity;
    private static final ArmRpoHelperMocks ARM_RPO_HELPER_MOCKS = new ArmRpoHelperMocks();
    private final Duration pollDuration = Duration.ofHours(4);

    private ArmRpoPollServiceImpl armRpoPollService;

    @BeforeEach
    void setUp() {
        armRpoPollService = new ArmRpoPollServiceImpl(armApiService, armRpoService, userIdentity, fileOperationService,
                                                      armDataManagementConfiguration, logApi, armRpoUtil, getExtendedSearchesByMatterService,
                                                      getMasterIndexFieldByRecordClassSchemaService,
                                                      createExportBasedOnSearchResultsTableService, getExtendedProductionsByMatterService,
                                                      getProductionOutputFilesService, downloadProductionService, removeProductionService,
                                                      tempProductionFiles, allowableFailedStates, inProgressStates);

        lenient().when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(EXECUTION_ID);
        when(armRpoService.getLatestArmRpoExecutionDetailEntity()).thenReturn(armRpoExecutionDetailEntity);

        lenient().when(armRpoUtil.generateUniqueProductionName(anyString())).thenReturn(PRODUCTION_NAME + "_UUID_CSV");
    }

    @Test
    void pollArmRpo_shouldPollSuccessfully_whenSaveBackgroundCompleted() throws IOException {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getSaveBackgroundSearchRpoState());

        String bearerToken = "bearerToken";
        when(armApiService.getArmBearerToken()).thenReturn(bearerToken);
        when(getExtendedSearchesByMatterService.getExtendedSearchesByMatter(anyString(), anyInt(), any(UserAccountEntity.class))).thenReturn(PRODUCTION_NAME);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();
        when(getMasterIndexFieldByRecordClassSchemaService.getMasterIndexFieldByRecordClassSchema(anyString(), anyInt(), any(ArmRpoStateEntity.class),
                                                                                                  any(UserAccountEntity.class)))
            .thenReturn(headerColumns);
        when(createExportBasedOnSearchResultsTableService.createExportBasedOnSearchResultsTable(anyString(), anyInt(), any(), anyString(), any(),
                                                                                                any(UserAccountEntity.class))).thenReturn(true);
        when(getExtendedProductionsByMatterService.getExtendedProductionsByMatter(anyString(), anyInt(), anyString(), any(UserAccountEntity.class)))
            .thenReturn(true);
        when(getProductionOutputFilesService.getProductionOutputFiles(anyString(), anyInt(), any(UserAccountEntity.class))).thenReturn(List.of("fileId"));
        InputStream resource = IOUtils.toInputStream("dummy input stream", "UTF-8");
        when(downloadProductionService.downloadProduction(anyString(), anyInt(), anyString(), any(UserAccountEntity.class))).thenReturn(resource);
        doNothing().when(removeProductionService).removeProduction(anyString(), anyInt(), any(UserAccountEntity.class));

        String fileName = "fileId.csv";
        Path filePath = Path.of(tempDirectory.getAbsolutePath()).resolve(fileName);
        when(fileOperationService.saveFileToTempWorkspace(any(InputStream.class), anyString(), any(), anyBoolean())).thenReturn(filePath);

        // when
        armRpoPollService.pollArmRpo(false, pollDuration, BATCH_SIZE);

        // then
        verify(getExtendedSearchesByMatterService).getExtendedSearchesByMatter("bearerToken", 1, userAccountEntity);
        verify(getMasterIndexFieldByRecordClassSchemaService).getMasterIndexFieldByRecordClassSchema(
            "bearerToken", 1, ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState(), userAccountEntity);
        verify(createExportBasedOnSearchResultsTableService).createExportBasedOnSearchResultsTable(eq("bearerToken"), eq(1), eq(headerColumns),
                                                                                                   contains(PRODUCTION_NAME), eq(pollDuration),
                                                                                                   eq(userAccountEntity));
        verify(getExtendedProductionsByMatterService).getExtendedProductionsByMatter(eq("bearerToken"), eq(1), contains(PRODUCTION_NAME),
                                                                                     eq(userAccountEntity));
        verify(getProductionOutputFilesService).getProductionOutputFiles("bearerToken", 1, userAccountEntity);
        verify(downloadProductionService).downloadProduction("bearerToken", 1, "fileId", userAccountEntity);
        verify(removeProductionService).removeProduction("bearerToken", 1, userAccountEntity);

        verify(userIdentity).getUserAccount();

        verify(fileOperationService).saveFileToTempWorkspace(resource, "productionExportFileId_fileId.csv", armDataManagementConfiguration, true);

        verify(logApi).armRpoPollingSuccessful(EXECUTION_ID);

        verifyInteractionsDone();
    }

    private void verifyInteractionsDone() {
        verifyNoMoreInteractions(getExtendedSearchesByMatterService,
                                 getMasterIndexFieldByRecordClassSchemaService,
                                 createExportBasedOnSearchResultsTableService, getExtendedProductionsByMatterService,
                                 getProductionOutputFilesService, downloadProductionService, removeProductionService,
                                 userIdentity, fileOperationService, logApi);
    }

    @Test
    void pollArmRpo_shouldPollSuccessfully_whenSaveBackgroundCompletedForManualRun() throws IOException {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getSaveBackgroundSearchRpoState());

        when(armApiService.getArmBearerToken()).thenReturn("bearerToken");
        when(getExtendedSearchesByMatterService.getExtendedSearchesByMatter(anyString(), anyInt(), any(UserAccountEntity.class))).thenReturn(PRODUCTION_NAME);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();
        when(getMasterIndexFieldByRecordClassSchemaService.getMasterIndexFieldByRecordClassSchema(anyString(), anyInt(), any(ArmRpoStateEntity.class),
                                                                                                  any(UserAccountEntity.class)))
            .thenReturn(headerColumns);
        when(createExportBasedOnSearchResultsTableService.createExportBasedOnSearchResultsTable(anyString(), anyInt(), any(), anyString(), any(),
                                                                                                any(UserAccountEntity.class))).thenReturn(true);
        when(getExtendedProductionsByMatterService.getExtendedProductionsByMatter(anyString(), anyInt(), anyString(), any(UserAccountEntity.class)))
            .thenReturn(true);
        when(getProductionOutputFilesService.getProductionOutputFiles(anyString(), anyInt(), any())).thenReturn(List.of("fileId"));
        InputStream resource = IOUtils.toInputStream("dummy input stream", "UTF-8");
        when(downloadProductionService.downloadProduction(anyString(), anyInt(), anyString(), any(UserAccountEntity.class))).thenReturn(resource);
        doNothing().when(removeProductionService).removeProduction(anyString(), anyInt(), any(UserAccountEntity.class));

        String fileName = "fileId.csv";
        Path filePath = Path.of(tempDirectory.getAbsolutePath()).resolve(fileName);
        when(fileOperationService.saveFileToTempWorkspace(any(InputStream.class), anyString(), any(), anyBoolean())).thenReturn(filePath);

        // when
        armRpoPollService.pollArmRpo(true, pollDuration, BATCH_SIZE);

        // then
        verify(getExtendedSearchesByMatterService).getExtendedSearchesByMatter("bearerToken", 1, userAccountEntity);
        verify(getMasterIndexFieldByRecordClassSchemaService).getMasterIndexFieldByRecordClassSchema(
            "bearerToken", 1, ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState(), userAccountEntity);
        verify(createExportBasedOnSearchResultsTableService).createExportBasedOnSearchResultsTable(eq("bearerToken"), eq(1), eq(headerColumns),
                                                                                                   contains(PRODUCTION_NAME), eq(pollDuration),
                                                                                                   eq(userAccountEntity));
        verify(getExtendedProductionsByMatterService).getExtendedProductionsByMatter(eq("bearerToken"), eq(1), contains(PRODUCTION_NAME),
                                                                                     eq(userAccountEntity));
        verify(getProductionOutputFilesService).getProductionOutputFiles("bearerToken", 1, userAccountEntity);
        verify(downloadProductionService).downloadProduction("bearerToken", 1, "fileId", userAccountEntity);
        verify(removeProductionService).removeProduction("bearerToken", 1, userAccountEntity);

        verify(userIdentity).getUserAccount();

        verify(fileOperationService).saveFileToTempWorkspace(resource, "productionExportFileId_fileId.csv", armDataManagementConfiguration, true);

        verify(logApi).armRpoPollingSuccessful(EXECUTION_ID);

        verifyInteractionsDone();
    }

    @Test
    void pollArmRpo_shouldPollSuccessfully_whenGetExtendedSearchesByMatterInProgress() throws IOException {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getGetExtendedSearchesByMatterRpoState());

        when(armApiService.getArmBearerToken()).thenReturn("bearerToken");
        when(getExtendedSearchesByMatterService.getExtendedSearchesByMatter(anyString(), anyInt(), any(UserAccountEntity.class))).thenReturn(PRODUCTION_NAME);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();
        when(getMasterIndexFieldByRecordClassSchemaService.getMasterIndexFieldByRecordClassSchema(anyString(), anyInt(), any(ArmRpoStateEntity.class),
                                                                                                  any(UserAccountEntity.class))).thenReturn(
            headerColumns);
        when(createExportBasedOnSearchResultsTableService.createExportBasedOnSearchResultsTable(anyString(), anyInt(), any(), anyString(), any(),
                                                                                                any(UserAccountEntity.class))).thenReturn(true);
        when(getExtendedProductionsByMatterService.getExtendedProductionsByMatter(anyString(), anyInt(), anyString(), any(UserAccountEntity.class))).thenReturn(
            true);
        when(getProductionOutputFilesService.getProductionOutputFiles(anyString(), anyInt(), any())).thenReturn(List.of("fileId"));
        InputStream resource = IOUtils.toInputStream("dummy input stream", "UTF-8");
        when(downloadProductionService.downloadProduction(anyString(), anyInt(), anyString(), any(UserAccountEntity.class))).thenReturn(resource);
        doNothing().when(removeProductionService).removeProduction(anyString(), anyInt(), any(UserAccountEntity.class));

        String fileName = "fileId.csv";
        Path filePath = Path.of(tempDirectory.getAbsolutePath()).resolve(fileName);
        when(fileOperationService.saveFileToTempWorkspace(any(InputStream.class), anyString(), any(), anyBoolean())).thenReturn(filePath);

        // when
        armRpoPollService.pollArmRpo(false, pollDuration, BATCH_SIZE);

        // then
        verify(getExtendedSearchesByMatterService).getExtendedSearchesByMatter("bearerToken", 1, userAccountEntity);
        verify(getMasterIndexFieldByRecordClassSchemaService).getMasterIndexFieldByRecordClassSchema(
            "bearerToken", 1, ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState(), userAccountEntity);
        verify(createExportBasedOnSearchResultsTableService).createExportBasedOnSearchResultsTable(
            eq("bearerToken"), eq(1), eq(headerColumns), contains(PRODUCTION_NAME), eq(pollDuration), eq(userAccountEntity));
        verify(getExtendedProductionsByMatterService).getExtendedProductionsByMatter(
            eq("bearerToken"), eq(1), contains(PRODUCTION_NAME), eq(userAccountEntity));
        verify(getProductionOutputFilesService).getProductionOutputFiles("bearerToken", 1, userAccountEntity);
        verify(downloadProductionService).downloadProduction("bearerToken", 1, "fileId", userAccountEntity);
        verify(removeProductionService).removeProduction("bearerToken", 1, userAccountEntity);

        verify(userIdentity).getUserAccount();

        verify(fileOperationService).saveFileToTempWorkspace(resource, "productionExportFileId_fileId.csv", armDataManagementConfiguration, true);

        verify(logApi).armRpoPollingSuccessful(EXECUTION_ID);

        verifyInteractionsDone();
    }

    @Test
    void pollArmRpo_shouldPollSuccessfully_whenCreateExportBasedOnSearchResultsTableInProgress() throws IOException {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getCreateExportBasedOnSearchResultsTableRpoState());

        when(armApiService.getArmBearerToken()).thenReturn("bearerToken");
        when(getExtendedSearchesByMatterService.getExtendedSearchesByMatter(anyString(), anyInt(), any(UserAccountEntity.class))).thenReturn(PRODUCTION_NAME);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();
        when(getMasterIndexFieldByRecordClassSchemaService.getMasterIndexFieldByRecordClassSchema(anyString(), anyInt(), any(ArmRpoStateEntity.class),
                                                                                                  any(UserAccountEntity.class))).thenReturn(
            headerColumns);
        when(createExportBasedOnSearchResultsTableService.createExportBasedOnSearchResultsTable(anyString(), anyInt(), any(), anyString(), any(),
                                                                                                any(UserAccountEntity.class))).thenReturn(true);
        when(getExtendedProductionsByMatterService.getExtendedProductionsByMatter(anyString(), anyInt(), anyString(), any(UserAccountEntity.class)))
            .thenReturn(true);
        when(getProductionOutputFilesService.getProductionOutputFiles(anyString(), anyInt(), any())).thenReturn(List.of("fileId"));
        InputStream resource = IOUtils.toInputStream("dummy input stream", "UTF-8");
        when(downloadProductionService.downloadProduction(anyString(), anyInt(), anyString(), any(UserAccountEntity.class))).thenReturn(resource);
        doNothing().when(removeProductionService).removeProduction(anyString(), anyInt(), any(UserAccountEntity.class));

        String fileName = "fileId.csv";
        Path filePath = Path.of(tempDirectory.getAbsolutePath()).resolve(fileName);
        when(fileOperationService.saveFileToTempWorkspace(any(InputStream.class), anyString(), any(), anyBoolean())).thenReturn(filePath);

        // when
        armRpoPollService.pollArmRpo(false, pollDuration, BATCH_SIZE);

        // then
        verify(getExtendedSearchesByMatterService).getExtendedSearchesByMatter("bearerToken", 1, userAccountEntity);
        verify(getMasterIndexFieldByRecordClassSchemaService).getMasterIndexFieldByRecordClassSchema(
            "bearerToken", 1, ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState(), userAccountEntity);
        verify(createExportBasedOnSearchResultsTableService).createExportBasedOnSearchResultsTable(
            eq("bearerToken"), eq(1), eq(headerColumns), contains(PRODUCTION_NAME), eq(pollDuration), eq(userAccountEntity));
        verify(getExtendedProductionsByMatterService).getExtendedProductionsByMatter(
            eq("bearerToken"), eq(1), contains(PRODUCTION_NAME), eq(userAccountEntity));
        verify(getProductionOutputFilesService).getProductionOutputFiles("bearerToken", 1, userAccountEntity);
        verify(downloadProductionService).downloadProduction("bearerToken", 1, "fileId", userAccountEntity);
        verify(removeProductionService).removeProduction("bearerToken", 1, userAccountEntity);

        verify(userIdentity).getUserAccount();

        verify(fileOperationService).saveFileToTempWorkspace(resource, "productionExportFileId_fileId.csv", armDataManagementConfiguration, true);

        verify(logApi).armRpoPollingSuccessful(EXECUTION_ID);

        verifyInteractionsDone();
    }

    @Test
    void pollArmRpo_shouldPollSuccessfully_whenGetExtendedProductionsByMatterInProgressAndSkipsSteps() throws IOException {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getGetExtendedProductionsByMatterRpoState());
        armRpoExecutionDetailEntity.setProductionName(PRODUCTION_NAME);
        armRpoExecutionDetailEntity.setPollingCreatedAt(OffsetDateTime.now().minusMinutes(10));

        when(armApiService.getArmBearerToken()).thenReturn("bearerToken");
        when(getExtendedProductionsByMatterService.getExtendedProductionsByMatter(anyString(), anyInt(), anyString(), any(UserAccountEntity.class)))
            .thenReturn(true);
        when(getProductionOutputFilesService.getProductionOutputFiles(anyString(), anyInt(), any())).thenReturn(List.of("fileId"));
        InputStream resource = IOUtils.toInputStream("dummy input stream", "UTF-8");
        when(downloadProductionService.downloadProduction(anyString(), anyInt(), anyString(), any(UserAccountEntity.class))).thenReturn(resource);
        doNothing().when(removeProductionService).removeProduction(anyString(), anyInt(), any(UserAccountEntity.class));

        String fileName = "fileId.csv";
        Path filePath = Path.of(tempDirectory.getAbsolutePath()).resolve(fileName);
        when(fileOperationService.saveFileToTempWorkspace(any(InputStream.class), anyString(), any(), anyBoolean())).thenReturn(filePath);

        // when
        armRpoPollService.pollArmRpo(false, pollDuration, BATCH_SIZE);

        // then
        verify(getExtendedProductionsByMatterService).getExtendedProductionsByMatter(eq("bearerToken"), eq(1), contains(PRODUCTION_NAME),
                                                                                     eq(userAccountEntity));
        verify(getProductionOutputFilesService).getProductionOutputFiles("bearerToken", 1, userAccountEntity);
        verify(downloadProductionService).downloadProduction("bearerToken", 1, "fileId", userAccountEntity);
        verify(removeProductionService).removeProduction("bearerToken", 1, userAccountEntity);

        verify(userIdentity).getUserAccount();

        verify(fileOperationService).saveFileToTempWorkspace(resource, "productionExportFileId_fileId.csv", armDataManagementConfiguration, true);

        verify(logApi).armRpoPollingSuccessful(EXECUTION_ID);

        verifyInteractionsDone();
    }

    @Test
    void pollArmRpo_shouldPollSuccessfully_whenCreateExportBasedOnSearchResultsTableForManualRun() throws IOException {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getCreateExportBasedOnSearchResultsTableRpoState());

        when(armApiService.getArmBearerToken()).thenReturn("bearerToken");
        when(getExtendedSearchesByMatterService.getExtendedSearchesByMatter(anyString(), anyInt(), any(UserAccountEntity.class))).thenReturn(PRODUCTION_NAME);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();
        when(getMasterIndexFieldByRecordClassSchemaService.getMasterIndexFieldByRecordClassSchema(anyString(), anyInt(), any(ArmRpoStateEntity.class),
                                                                                                  any(UserAccountEntity.class)))
            .thenReturn(headerColumns);
        when(createExportBasedOnSearchResultsTableService.createExportBasedOnSearchResultsTable(anyString(), anyInt(), any(), anyString(), any(),
                                                                                                any(UserAccountEntity.class))).thenReturn(true);
        when(getExtendedProductionsByMatterService.getExtendedProductionsByMatter(anyString(), anyInt(), anyString(), any(UserAccountEntity.class)))
            .thenReturn(true);
        when(getProductionOutputFilesService.getProductionOutputFiles(anyString(), anyInt(), any())).thenReturn(List.of("fileId"));
        InputStream resource = IOUtils.toInputStream("dummy input stream", "UTF-8");
        when(downloadProductionService.downloadProduction(anyString(), anyInt(), anyString(), any(UserAccountEntity.class))).thenReturn(resource);
        doNothing().when(removeProductionService).removeProduction(anyString(), anyInt(), any(UserAccountEntity.class));

        String fileName = "fileId.csv";
        Path filePath = Path.of(tempDirectory.getAbsolutePath()).resolve(fileName);
        when(fileOperationService.saveFileToTempWorkspace(any(InputStream.class), anyString(), any(), anyBoolean())).thenReturn(filePath);

        // when
        armRpoPollService.pollArmRpo(true, pollDuration, BATCH_SIZE);

        // then
        verify(getExtendedSearchesByMatterService).getExtendedSearchesByMatter("bearerToken", 1, userAccountEntity);
        verify(getMasterIndexFieldByRecordClassSchemaService).getMasterIndexFieldByRecordClassSchema(
            "bearerToken", 1, ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState(), userAccountEntity);
        verify(createExportBasedOnSearchResultsTableService).createExportBasedOnSearchResultsTable(
            eq("bearerToken"), eq(1), eq(headerColumns), contains(PRODUCTION_NAME), eq(pollDuration), eq(userAccountEntity));
        verify(getExtendedProductionsByMatterService).getExtendedProductionsByMatter(
            eq("bearerToken"), eq(1), contains(PRODUCTION_NAME), eq(userAccountEntity));
        verify(getProductionOutputFilesService).getProductionOutputFiles("bearerToken", 1, userAccountEntity);
        verify(downloadProductionService).downloadProduction("bearerToken", 1, "fileId", userAccountEntity);
        verify(removeProductionService).removeProduction("bearerToken", 1, userAccountEntity);

        verify(userIdentity).getUserAccount();

        verify(fileOperationService).saveFileToTempWorkspace(resource, "productionExportFileId_fileId.csv", armDataManagementConfiguration, true);

        verify(logApi).armRpoPollingSuccessful(EXECUTION_ID);

        verifyInteractionsDone();
    }

    @Test
    void pollArmRpo_shouldPollSuccessfully_whenDownloadProductionFailedOnPreviousAttemptForManualRun() throws IOException {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getDownloadProductionRpoState());

        when(armApiService.getArmBearerToken()).thenReturn("bearerToken");
        when(getExtendedSearchesByMatterService.getExtendedSearchesByMatter(anyString(), anyInt(), any(UserAccountEntity.class))).thenReturn(PRODUCTION_NAME);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();
        when(getMasterIndexFieldByRecordClassSchemaService.getMasterIndexFieldByRecordClassSchema(anyString(), anyInt(), any(ArmRpoStateEntity.class),
                                                                                                  any(UserAccountEntity.class)))
            .thenReturn(headerColumns);
        when(createExportBasedOnSearchResultsTableService.createExportBasedOnSearchResultsTable(anyString(), anyInt(), any(), anyString(), any(),
                                                                                                any(UserAccountEntity.class))).thenReturn(true);
        when(getExtendedProductionsByMatterService.getExtendedProductionsByMatter(anyString(), anyInt(), anyString(), any(UserAccountEntity.class)))
            .thenReturn(true);
        when(getProductionOutputFilesService.getProductionOutputFiles(anyString(), anyInt(), any())).thenReturn(List.of("fileId"));
        InputStream resource = IOUtils.toInputStream("dummy input stream", "UTF-8");
        when(downloadProductionService.downloadProduction(anyString(), anyInt(), anyString(), any(UserAccountEntity.class))).thenReturn(resource);
        doNothing().when(removeProductionService).removeProduction(anyString(), anyInt(), any(UserAccountEntity.class));

        String fileName = "fileId.csv";
        Path filePath = Path.of(tempDirectory.getAbsolutePath()).resolve(fileName);
        when(fileOperationService.saveFileToTempWorkspace(any(InputStream.class), anyString(), any(), anyBoolean())).thenReturn(filePath);

        // when
        armRpoPollService.pollArmRpo(true, pollDuration, BATCH_SIZE);

        // then
        verify(getExtendedSearchesByMatterService).getExtendedSearchesByMatter("bearerToken", 1, userAccountEntity);
        verify(getMasterIndexFieldByRecordClassSchemaService).getMasterIndexFieldByRecordClassSchema(
            "bearerToken", 1, ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState(), userAccountEntity);
        verify(createExportBasedOnSearchResultsTableService).createExportBasedOnSearchResultsTable(
            eq("bearerToken"), eq(1), eq(headerColumns), contains(PRODUCTION_NAME), eq(pollDuration), eq(userAccountEntity));
        verify(getExtendedProductionsByMatterService).getExtendedProductionsByMatter(
            eq("bearerToken"), eq(1), contains(PRODUCTION_NAME), eq(userAccountEntity));
        verify(getProductionOutputFilesService).getProductionOutputFiles("bearerToken", 1, userAccountEntity);
        verify(downloadProductionService).downloadProduction("bearerToken", 1, "fileId", userAccountEntity);
        verify(removeProductionService).removeProduction("bearerToken", 1, userAccountEntity);

        verify(userIdentity).getUserAccount();

        verify(fileOperationService).saveFileToTempWorkspace(resource, "productionExportFileId_fileId.csv", armDataManagementConfiguration, true);

        verify(logApi).armRpoPollingSuccessful(EXECUTION_ID);

        verifyInteractionsDone();
    }

    @Test
    void pollArmRpo_shouldPollNotFindLatestExecutionDetail_OnStepDownloadProductionFailed() {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getDownloadProductionRpoState());

        // when
        armRpoPollService.pollArmRpo(false, pollDuration, BATCH_SIZE);

        // then
        verify(armRpoService).getLatestArmRpoExecutionDetailEntity();

        verifyInteractionsDone();
    }

    @Test
    void pollArmRpo_shouldPollNotFindLatestExecutionDetailForManualRun_OnStepDownloadProductionInProgress() {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getDownloadProductionRpoState());

        // when
        armRpoPollService.pollArmRpo(true, pollDuration, BATCH_SIZE);

        // then
        verify(armRpoService).getLatestArmRpoExecutionDetailEntity();

        verifyInteractionsDone();
    }

    @Test
    void pollArmRpo_shouldHandleNoExecutionDetailEntity() {
        // given
        when(armRpoService.getLatestArmRpoExecutionDetailEntity()).thenReturn(null);

        // when
        armRpoPollService.pollArmRpo(false, pollDuration, BATCH_SIZE);

        // then
        verify(armRpoService).getLatestArmRpoExecutionDetailEntity();

        verifyInteractionsDone();
    }

    @Test
    void pollArmRpo_shouldHandleNoBearerToken() {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getSaveBackgroundSearchRpoState());
        when(armRpoService.getLatestArmRpoExecutionDetailEntity()).thenReturn(armRpoExecutionDetailEntity);
        when(armApiService.getArmBearerToken()).thenReturn(null);

        // when
        armRpoPollService.pollArmRpo(false, pollDuration, BATCH_SIZE);

        // then
        verify(armRpoService).getLatestArmRpoExecutionDetailEntity();
        verify(armApiService).getArmBearerToken();
        verify(logApi).armRpoPollingFailed(EXECUTION_ID);
        verifyInteractionsDone();
    }

    @Test
    void pollArmRpo_shouldHandleCreateExportInProgress() {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getSaveBackgroundSearchRpoState());
        when(armApiService.getArmBearerToken()).thenReturn("bearerToken");
        when(getExtendedSearchesByMatterService.getExtendedSearchesByMatter(anyString(), anyInt(), any(UserAccountEntity.class))).thenReturn(PRODUCTION_NAME);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();
        when(getMasterIndexFieldByRecordClassSchemaService.getMasterIndexFieldByRecordClassSchema(anyString(), anyInt(), any(ArmRpoStateEntity.class),
                                                                                                  any(UserAccountEntity.class)))
            .thenReturn(headerColumns);
        when(createExportBasedOnSearchResultsTableService.createExportBasedOnSearchResultsTable(anyString(), anyInt(), any(), anyString(), any(),
                                                                                                any(UserAccountEntity.class))).thenReturn(false);

        // when
        armRpoPollService.pollArmRpo(false, pollDuration, BATCH_SIZE);

        // then
        verify(armRpoService).getLatestArmRpoExecutionDetailEntity();
        verify(armApiService).getArmBearerToken();
        verify(getExtendedSearchesByMatterService).getExtendedSearchesByMatter("bearerToken", 1, userAccountEntity);
        verify(getMasterIndexFieldByRecordClassSchemaService).getMasterIndexFieldByRecordClassSchema(
            "bearerToken", 1, ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState(), userAccountEntity);
        verify(createExportBasedOnSearchResultsTableService).createExportBasedOnSearchResultsTable(
            eq("bearerToken"), eq(1), eq(headerColumns), contains(PRODUCTION_NAME), eq(pollDuration), eq(userAccountEntity));
        verify(userIdentity).getUserAccount();

        verifyInteractionsDone();
    }

    @Test
    void pollArmRpo_shouldHandleGetExtendedSearchesByMatterGetInProgress() {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getSaveBackgroundSearchRpoState());
        when(armApiService.getArmBearerToken()).thenReturn("bearerToken");
        when(getExtendedSearchesByMatterService.getExtendedSearchesByMatter(anyString(), anyInt(), any(UserAccountEntity.class))).thenThrow(
            ArmRpoInProgressException.class);

        // when
        armRpoPollService.pollArmRpo(false, pollDuration, BATCH_SIZE);

        // then
        verify(armRpoService).getLatestArmRpoExecutionDetailEntity();
        verify(armApiService).getArmBearerToken();
        verify(getExtendedSearchesByMatterService).getExtendedSearchesByMatter("bearerToken", 1, userAccountEntity);
        verify(userIdentity).getUserAccount();

        verifyInteractionsDone();
    }

    @Test
    void pollArmRpo_shouldHandleNoProductionFiles() {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getSaveBackgroundSearchRpoState());
        when(armApiService.getArmBearerToken()).thenReturn("bearerToken");
        when(getExtendedSearchesByMatterService.getExtendedSearchesByMatter(anyString(), anyInt(), any(UserAccountEntity.class))).thenReturn(PRODUCTION_NAME);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();
        when(getMasterIndexFieldByRecordClassSchemaService.getMasterIndexFieldByRecordClassSchema(anyString(), anyInt(), any(ArmRpoStateEntity.class),
                                                                                                  any(UserAccountEntity.class)))
            .thenReturn(headerColumns);
        when(createExportBasedOnSearchResultsTableService.createExportBasedOnSearchResultsTable(anyString(), anyInt(), any(), anyString(), any(),
                                                                                                any(UserAccountEntity.class))).thenReturn(true);
        when(getExtendedProductionsByMatterService.getExtendedProductionsByMatter(anyString(), anyInt(), anyString(), any(UserAccountEntity.class)))
            .thenReturn(true);
        when(getProductionOutputFilesService.getProductionOutputFiles(anyString(), anyInt(), any(UserAccountEntity.class))).thenReturn(List.of());

        // when
        armRpoPollService.pollArmRpo(false, pollDuration, BATCH_SIZE);

        // then
        verify(armRpoService).getLatestArmRpoExecutionDetailEntity();
        verify(armApiService).getArmBearerToken();
        verify(getExtendedSearchesByMatterService).getExtendedSearchesByMatter("bearerToken", 1, userAccountEntity);
        verify(getMasterIndexFieldByRecordClassSchemaService).getMasterIndexFieldByRecordClassSchema(
            "bearerToken", 1, ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState(), userAccountEntity);
        verify(createExportBasedOnSearchResultsTableService).createExportBasedOnSearchResultsTable(
            eq("bearerToken"), eq(1), eq(headerColumns), contains(PRODUCTION_NAME), eq(pollDuration), eq(userAccountEntity));
        verify(getExtendedProductionsByMatterService).getExtendedProductionsByMatter(
            eq("bearerToken"), eq(1), contains(PRODUCTION_NAME), eq(userAccountEntity));
        verify(getProductionOutputFilesService).getProductionOutputFiles("bearerToken", 1, userAccountEntity);
        verify(userIdentity).getUserAccount();
        verify(logApi).armRpoPollingSuccessful(EXECUTION_ID);

        verifyInteractionsDone();
    }

    @Test
    void pollArmRpo_shouldHandleExceptionDuringPolling_whenCreateExportBasedOnSearchResultsTableStep() {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getSaveBackgroundSearchRpoState());
        when(armApiService.getArmBearerToken()).thenReturn("bearerToken");
        when(getExtendedSearchesByMatterService.getExtendedSearchesByMatter(anyString(), anyInt(), any(UserAccountEntity.class))).thenReturn(PRODUCTION_NAME);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();
        when(getMasterIndexFieldByRecordClassSchemaService.getMasterIndexFieldByRecordClassSchema(anyString(), anyInt(), any(ArmRpoStateEntity.class),
                                                                                                  any(UserAccountEntity.class)))
            .thenReturn(headerColumns);
        when(createExportBasedOnSearchResultsTableService.createExportBasedOnSearchResultsTable(anyString(), anyInt(), any(), anyString(), any(),
                                                                                                any(UserAccountEntity.class)))
            .thenThrow(new ArmRpoException("Test exception"));

        // when
        armRpoPollService.pollArmRpo(false, pollDuration, BATCH_SIZE);

        // then
        verify(armRpoService).getLatestArmRpoExecutionDetailEntity();
        verify(armApiService).getArmBearerToken();
        verify(getExtendedSearchesByMatterService).getExtendedSearchesByMatter("bearerToken", 1, userAccountEntity);
        verify(getMasterIndexFieldByRecordClassSchemaService).getMasterIndexFieldByRecordClassSchema(
            "bearerToken", 1, ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState(), userAccountEntity);
        verify(createExportBasedOnSearchResultsTableService).createExportBasedOnSearchResultsTable(
            eq("bearerToken"), eq(1), eq(headerColumns), contains(PRODUCTION_NAME), eq(pollDuration), eq(userAccountEntity));
        verify(userIdentity).getUserAccount();
        verify(logApi).armRpoPollingFailed(EXECUTION_ID);

        verifyInteractionsDone();
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