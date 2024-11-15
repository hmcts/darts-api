package uk.gov.hmcts.darts.arm.service.impl;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelperMocks;
import uk.gov.hmcts.darts.arm.model.rpo.MasterIndexFieldByRecordClassSchema;
import uk.gov.hmcts.darts.arm.rpo.ArmRpoApi;
import uk.gov.hmcts.darts.arm.service.ArmApiService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.service.FileOperationService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
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
class ArmRpoPollServiceImplTest {

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
    private UserAccountEntity userAccountEntity;

    @TempDir
    private File tempDirectory;

    private static final Integer EXECUTION_ID = 1;
    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity;
    private static final ArmRpoHelperMocks ARM_RPO_HELPER_MOCKS = new ArmRpoHelperMocks();

    @InjectMocks
    private ArmRpoPollServiceImpl armRpoPollService;


    @BeforeEach
    void setUp() {
        lenient().when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(EXECUTION_ID);
        when(armRpoService.getLatestArmRpoExecutionDetailEntity()).thenReturn(armRpoExecutionDetailEntity);
    }

    @Test
    void pollArmRpo_shouldPollSuccessfullyWithSaveBackgroundCompleted() throws IOException {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getSaveBackgroundSearchRpoState());

        when(armApiService.getArmBearerToken()).thenReturn("bearerToken");
        doNothing().when(armRpoApi).getExtendedSearchesByMatter(anyString(), anyInt(), any());
        when(armRpoApi.getMasterIndexFieldByRecordClassSchema(anyString(), anyInt(), any(), any()))
            .thenReturn(createHeaderColumns());
        when(armRpoApi.createExportBasedOnSearchResultsTable(anyString(), anyInt(), any(), any())).thenReturn(true);
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
        verify(armRpoApi).getExtendedSearchesByMatter(anyString(), anyInt(), any());
        verify(armRpoApi).getMasterIndexFieldByRecordClassSchema(anyString(), anyInt(), any(), any());
        verify(armRpoApi).createExportBasedOnSearchResultsTable(anyString(), anyInt(), any(), any());
        verify(armRpoApi).getExtendedProductionsByMatter(anyString(), anyInt(), any());
        verify(armRpoApi).getProductionOutputFiles(anyString(), anyInt(), any());
        verify(armRpoApi).downloadProduction(anyString(), anyInt(), any(), any());
        verify(armRpoApi).removeProduction(anyString(), anyInt(), any());
    }

    @Test
    void pollArmRpo_shouldPollSuccessfullyWithCreateExportBasedOnSearchResultsTableInProgress() throws IOException {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getCreateExportBasedOnSearchResultsTableRpoState());

        when(armApiService.getArmBearerToken()).thenReturn("bearerToken");
        doNothing().when(armRpoApi).getExtendedSearchesByMatter(anyString(), anyInt(), any());
        when(armRpoApi.getMasterIndexFieldByRecordClassSchema(anyString(), anyInt(), any(), any()))
            .thenReturn(createHeaderColumns());
        when(armRpoApi.createExportBasedOnSearchResultsTable(anyString(), anyInt(), any(), any())).thenReturn(true);
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
        verify(armRpoApi).getExtendedSearchesByMatter(anyString(), anyInt(), any());
        verify(armRpoApi).getMasterIndexFieldByRecordClassSchema(anyString(), anyInt(), any(), any());
        verify(armRpoApi).createExportBasedOnSearchResultsTable(anyString(), anyInt(), any(), any());
        verify(armRpoApi).getExtendedProductionsByMatter(anyString(), anyInt(), any());
        verify(armRpoApi).getProductionOutputFiles(anyString(), anyInt(), any());
        verify(armRpoApi).downloadProduction(anyString(), anyInt(), any(), any());
        verify(armRpoApi).removeProduction(anyString(), anyInt(), any());
    }

    @Test
    void pollArmRpo_shouldHandleNoExecutionDetailEntity() {
        // given
        when(armRpoService.getLatestArmRpoExecutionDetailEntity()).thenReturn(null);

        // when
        armRpoPollService.pollArmRpo(false);

        // then
        verify(armRpoService).getLatestArmRpoExecutionDetailEntity();
        verifyNoMoreInteractions(armRpoApi, armApiService, userIdentity, fileOperationService);
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
        verifyNoMoreInteractions(armRpoApi, userIdentity, fileOperationService);
    }

    @Test
    void pollArmRpo_shouldHandleCreateExportInProgress() {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getSaveBackgroundSearchRpoState());
        when(armApiService.getArmBearerToken()).thenReturn("bearerToken");
        when(armRpoApi.createExportBasedOnSearchResultsTable(anyString(), anyInt(), any(), any())).thenReturn(false);

        // when
        armRpoPollService.pollArmRpo(false);

        // then
        verify(armRpoService).getLatestArmRpoExecutionDetailEntity();
        verify(armApiService).getArmBearerToken();
        verify(armRpoApi).getExtendedSearchesByMatter(anyString(), anyInt(), any());
        verify(armRpoApi).getMasterIndexFieldByRecordClassSchema(anyString(), anyInt(), any(), any());
        verify(armRpoApi).createExportBasedOnSearchResultsTable(anyString(), anyInt(), any(), any());
        verify(userIdentity).getUserAccount();
        verifyNoMoreInteractions(armRpoApi, userIdentity, fileOperationService);
    }

    @Test
    void pollArmRpo_shouldHandleNoProductionFiles() {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getSaveBackgroundSearchRpoState());
        when(armApiService.getArmBearerToken()).thenReturn("bearerToken");
        when(armRpoApi.createExportBasedOnSearchResultsTable(anyString(), anyInt(), any(), any())).thenReturn(true);
        when(armRpoApi.getProductionOutputFiles(anyString(), anyInt(), any())).thenReturn(List.of());

        // when
        armRpoPollService.pollArmRpo(false);

        // then
        verify(armRpoService).getLatestArmRpoExecutionDetailEntity();
        verify(armApiService).getArmBearerToken();
        verify(armRpoApi).getExtendedSearchesByMatter(anyString(), anyInt(), any());
        verify(armRpoApi).getMasterIndexFieldByRecordClassSchema(anyString(), anyInt(), any(), any());
        verify(armRpoApi).createExportBasedOnSearchResultsTable(anyString(), anyInt(), any(), any());
        verify(armRpoApi).getExtendedProductionsByMatter(anyString(), anyInt(), any());
        verify(armRpoApi).getProductionOutputFiles(anyString(), anyInt(), any());
        verify(userIdentity).getUserAccount();
        verifyNoMoreInteractions(armRpoApi, userIdentity, fileOperationService);
    }

    @Test
    void pollArmRpo_shouldHandleExceptionDuringPollingOnCreateExportBasedOnSearchResultsTableStep() {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getSaveBackgroundSearchRpoState());
        when(armApiService.getArmBearerToken()).thenReturn("bearerToken");
        when(armRpoApi.createExportBasedOnSearchResultsTable(anyString(), anyInt(), any(), any())).thenThrow(new ArmRpoException("Test exception"));

        // when
        armRpoPollService.pollArmRpo(false);

        // then
        verify(armRpoService).getLatestArmRpoExecutionDetailEntity();
        verify(armApiService).getArmBearerToken();
        verify(armRpoApi).getExtendedSearchesByMatter(anyString(), anyInt(), any());
        verify(armRpoApi).getMasterIndexFieldByRecordClassSchema(anyString(), anyInt(), any(), any());
        verify(armRpoApi).createExportBasedOnSearchResultsTable(anyString(), anyInt(), any(), any());
        verify(userIdentity).getUserAccount();
        verifyNoMoreInteractions(armRpoApi, userIdentity, fileOperationService);
    }

    private List<MasterIndexFieldByRecordClassSchema> createHeaderColumns() {
        return List.of(
            createMasterIndexFieldByRecordClassSchema("200b9c27-b497-4977-82e7-1586b32a5871", "Record Class", "record_class", "string", false),
            createMasterIndexFieldByRecordClassSchema("90ee0e13-8639-4c4a-b542-66b6c8911549", "Archived Date", "ingestionDate", "date", false),
            createMasterIndexFieldByRecordClassSchema("a9b8daf2-d9ff-4815-b65a-f6ae2763b92c", "Client Identifier", "client_identifier", "string",
                                                      false),
            createMasterIndexFieldByRecordClassSchema("109b6bf1-57a0-48ec-b22e-c7248dc74f91", "Contributor", "contributor", "string", false),
            createMasterIndexFieldByRecordClassSchema("893048bf-1e7c-4811-9abf-00cd77a715cf", "Record Date", "recordDate", "date", false),
            createMasterIndexFieldByRecordClassSchema("fdd0fcbb-da46-4af1-a627-ac255c12bb23", "ObjectId", "bf_012", "number", false)
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