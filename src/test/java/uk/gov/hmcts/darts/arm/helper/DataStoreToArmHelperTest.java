package uk.gov.hmcts.darts.arm.helper;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.component.ArchiveRecordFileGenerator;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.model.batch.ArmBatchItems;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.testutils.ExternalObjectDirectoryTestData;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_INGESTION;

@ExtendWith(MockitoExtension.class)
class DataStoreToArmHelperTest {

    @InjectMocks
    private DataStoreToArmHelper dataStoreToArmHelper;

    @Mock
    private ObjectRecordStatusRepository objectRecordStatusRepository;
    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @Mock
    private ExternalLocationTypeRepository externalLocationTypeRepository;
    @Mock
    private LogApi logApi;
    @Mock
    private ArmDataManagementApi armDataManagementApi;
    @Mock
    private FileOperationService fileOperationService;
    @Mock
    private ArchiveRecordFileGenerator archiveRecordFileGenerator;

    @TempDir
    private File tempDirectory;

    private ExternalObjectDirectoryEntity externalObjectDirectoryEntity;

    private static final EodHelperMocks EOD_HELPER_MOCKS = new EodHelperMocks();

    @BeforeEach
    void setUp() {
        MediaEntity mediaEntity1 = new MediaEntity();
        externalObjectDirectoryEntity = new ExternalObjectDirectoryTestData().createExternalObjectDirectory(
            mediaEntity1,
            ARM,
            ARM_INGESTION,
            UUID.randomUUID());
        externalObjectDirectoryEntity.setId(345);
        externalObjectDirectoryEntity.setStatus(EodHelper.armIngestionStatus());
        externalObjectDirectoryEntity.setOsrUuid(1234L);
        externalObjectDirectoryEntity.getStatus().setDescription(ARM_INGESTION.name());
    }

    @AfterAll
    static void close() {
        EOD_HELPER_MOCKS.close();
    }

    @Test
    void testGetEodEntitiesToSendToArm() {
        // given
        ExternalLocationTypeEntity sourceLocation = mock(ExternalLocationTypeEntity.class);
        ExternalLocationTypeEntity armLocation = mock(ExternalLocationTypeEntity.class);
        ObjectRecordStatusEntity armRawStatusFailed = mock(ObjectRecordStatusEntity.class);
        ObjectRecordStatusEntity armManifestFailed = mock(ObjectRecordStatusEntity.class);
        when(objectRecordStatusRepository.getReferenceById(anyInt())).thenReturn(armRawStatusFailed).thenReturn(armManifestFailed);
        when(externalObjectDirectoryRepository.findNotFinishedAndNotExceededRetryInStorageLocation(anyList(), any(), anyInt(), any(Pageable.class)))
            .thenReturn(List.of(123));
        when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(3);

        // when
        List<Integer> result = dataStoreToArmHelper.getEodEntitiesToSendToArm(sourceLocation, armLocation, 5);

        // then
        assertNotNull(result);
        verify(externalObjectDirectoryRepository).findNotFinishedAndNotExceededRetryInStorageLocation(anyList(), any(), anyInt(), any(Pageable.class));
    }

    @Test
    void testGetExternalObjectDirectoryEntity() {
        // given
        ExternalLocationTypeEntity eodSourceLocation = mock(ExternalLocationTypeEntity.class);
        ObjectRecordStatusEntity status = mock(ObjectRecordStatusEntity.class);
        when(externalObjectDirectoryRepository.findMatchingExternalObjectDirectoryEntityByLocation(any(), any(), any(), any(), any(), any()))
            .thenReturn(Optional.of(externalObjectDirectoryEntity));

        // when
        Optional<ExternalObjectDirectoryEntity> result = dataStoreToArmHelper.getExternalObjectDirectoryEntity(externalObjectDirectoryEntity, eodSourceLocation,
                                                                                                               status);

        // then
        assertTrue(result.isPresent());
        verify(externalObjectDirectoryRepository).findMatchingExternalObjectDirectoryEntityByLocation(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testUpdateExternalObjectDirectoryStatusToFailed() {
        // given
        ObjectRecordStatusEntity objectRecordStatus = mock(ObjectRecordStatusEntity.class);
        UserAccountEntity userAccount = mock(UserAccountEntity.class);

        // when
        dataStoreToArmHelper.updateExternalObjectDirectoryStatusToFailed(externalObjectDirectoryEntity, objectRecordStatus, userAccount);

        // then
        verify(externalObjectDirectoryRepository).saveAndFlush(any());
    }

    @Test
    void testUpdateExternalObjectDirectoryStatus() {
        // given
        ObjectRecordStatusEntity armStatus = mock(ObjectRecordStatusEntity.class);
        UserAccountEntity userAccount = mock(UserAccountEntity.class);

        // when
        dataStoreToArmHelper.updateExternalObjectDirectoryStatus(externalObjectDirectoryEntity, armStatus, userAccount);

        // then
        verify(externalObjectDirectoryRepository).saveAndFlush(externalObjectDirectoryEntity);
    }

    @Test
    void generateRawFilename() {
        // given

        // when
        String result = dataStoreToArmHelper.generateRawFilename(externalObjectDirectoryEntity);

        // then
        assertNotNull(result);
    }

    @Test
    void copyUnstructuredRawDataToArm() {
        // given
        ExternalObjectDirectoryEntity unstructuredExternalObjectDirectory = mock(ExternalObjectDirectoryEntity.class);
        ExternalObjectDirectoryEntity armExternalObjectDirectory = mock(ExternalObjectDirectoryEntity.class);
        String filename = "testfile";
        ObjectRecordStatusEntity previousStatus = mock(ObjectRecordStatusEntity.class);
        UserAccountEntity userAccount = mock(UserAccountEntity.class);

        // when
        boolean result = dataStoreToArmHelper.copyUnstructuredRawDataToArm(unstructuredExternalObjectDirectory, armExternalObjectDirectory, filename,
                                                                           previousStatus, userAccount);

        // then
        assertTrue(result);
    }

    @Test
    void updateExternalObjectDirectoryFailedTransferAttempts() {
        // given
        UserAccountEntity userAccount = mock(UserAccountEntity.class);

        // when
        dataStoreToArmHelper.updateExternalObjectDirectoryFailedTransferAttempts(externalObjectDirectoryEntity, userAccount);

        // then
        verify(externalObjectDirectoryRepository).saveAndFlush(externalObjectDirectoryEntity);
    }

    @Test
    void createEmptyArchiveRecordsFile() throws IOException {
        // given
        String manifestFilePrefix = "DETS";
        when(fileOperationService.createFile(anyString(), anyString(), anyBoolean())).thenReturn(tempDirectory.toPath());
        when(armDataManagementConfiguration.getFileExtension()).thenReturn("a360");
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn("/temp_workspace");

        // when
        File result = dataStoreToArmHelper.createEmptyArchiveRecordsFile(manifestFilePrefix);

        // then
        assertNotNull(result);
    }

    @Test
    void writeManifestFile() {
        // given
        ArmBatchItems batchItems = mock(ArmBatchItems.class);
        File archiveRecordsFile = mock(File.class);

        // when
        dataStoreToArmHelper.writeManifestFile(batchItems, archiveRecordsFile);

        // then
        verify(archiveRecordFileGenerator).generateArchiveRecords(anyList(), any());
    }

    @Test
    void getFileSize() {
        // given
        when(externalObjectDirectoryRepository.findFileSize(anyInt())).thenReturn(1000L);

        // when
        Long result = dataStoreToArmHelper.getFileSize(externalObjectDirectoryEntity.getId());

        // then
        assertEquals(1000L, result);
    }
}