package uk.gov.hmcts.darts.arm.helper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.component.ArchiveRecordFileGenerator;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.model.ArchiveRecord;
import uk.gov.hmcts.darts.arm.model.batch.ArmBatchItem;
import uk.gov.hmcts.darts.arm.model.batch.ArmBatchItems;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.ExternalObjectDirectoryTestData;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_INGESTION;

@ExtendWith(MockitoExtension.class)
class DataStoreToArmHelperTest {

    private DataStoreToArmHelper dataStoreToArmHelper;

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @Mock
    private LogApi logApi;
    @Mock
    private ArmDataManagementApi armDataManagementApi;
    @Mock
    private ArchiveRecordFileGenerator archiveRecordFileGenerator;
    @Mock
    private ArchiveRecordService archiveRecordService;

    private ExternalObjectDirectoryEntity externalObjectDirectoryEntity;
    private EodHelperMocks eodHelperMocks;

    @BeforeEach
    void setUp() {
        eodHelperMocks = new EodHelperMocks(false);
        eodHelperMocks.simulateInitWithMockedData();

        dataStoreToArmHelper = new DataStoreToArmHelper(
            externalObjectDirectoryRepository,
            armDataManagementConfiguration,
            logApi,
            armDataManagementApi,
            archiveRecordFileGenerator,
            archiveRecordService
        );

        MediaEntity mediaEntity1 = PersistableFactory.getMediaTestData().someMinimal();
        mediaEntity1.setId(456L);
        externalObjectDirectoryEntity = new ExternalObjectDirectoryTestData().createExternalObjectDirectory(
            mediaEntity1,
            ARM,
            ARM_INGESTION,
            UUID.randomUUID().toString());
        externalObjectDirectoryEntity.setId(123L);
        externalObjectDirectoryEntity.setStatus(eodHelperMocks.getArmIngestionStatus());
        externalObjectDirectoryEntity.setOsrUuid(1234L);
        externalObjectDirectoryEntity.setTransferAttempts(1);

    }

    @AfterEach
    void close() {
        eodHelperMocks.close();
    }

    @Test
    void getEodEntitiesToSendToArm_ShouldSucceed() {
        // given
        ExternalLocationTypeEntity sourceLocation = eodHelperMocks.getUnstructuredLocation();
        ExternalLocationTypeEntity armLocation = eodHelperMocks.getArmLocation();

        when(externalObjectDirectoryRepository.findNotFinishedAndNotExceededRetryInStorageLocation(anyList(), any(), anyInt(), any(Pageable.class)))
            .thenReturn(List.of(123L));
        when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(3);

        // when
        List<Long> result = dataStoreToArmHelper.getEodEntitiesToSendToArm(sourceLocation, armLocation, 5);

        // then
        assertNotNull(result);
        verify(externalObjectDirectoryRepository).findNotFinishedAndNotExceededRetryInStorageLocation(anyList(), any(), anyInt(), any(Pageable.class));
    }

    @Test
    void getExternalObjectDirectoryEntity_ShouldSucceed() {
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
    void updateExternalObjectDirectoryStatusToFailed_ShouldSucceed() {
        // given
        ObjectRecordStatusEntity objectRecordStatus = eodHelperMocks.getArmResponseManifestFailedStatus();
        externalObjectDirectoryEntity.setStatus(eodHelperMocks.getArmIngestionStatus());
        UserAccountEntity userAccount = mock(UserAccountEntity.class);

        // when
        dataStoreToArmHelper.updateExternalObjectDirectoryStatusToFailed(externalObjectDirectoryEntity, objectRecordStatus, userAccount);

        // then
        verify(externalObjectDirectoryRepository).saveAndFlush(any());
    }

    @Test
    void updateExternalObjectDirectoryStatus_ShouldSucceed() {
        // given
        ObjectRecordStatusEntity armStatus = eodHelperMocks.getArmDropZoneStatus();
        UserAccountEntity userAccount = mock(UserAccountEntity.class);
        externalObjectDirectoryEntity.setStatus(eodHelperMocks.getArmIngestionStatus());

        // when
        dataStoreToArmHelper.updateExternalObjectDirectoryStatus(externalObjectDirectoryEntity, armStatus, userAccount);

        // then
        verify(externalObjectDirectoryRepository).saveAndFlush(externalObjectDirectoryEntity);
    }

    @Test
    void generateRawFilename_ShouldSucceed_WithMedia() {
        // when
        String result = dataStoreToArmHelper.generateRawFilename(externalObjectDirectoryEntity);

        // then
        assertEquals("Filenames don't match", "123_456_1", result);
    }

    @Test
    void generateRawFilename_ShouldSucceed_WithAnnotationDocument() {
        // given
        var annotationDocument = PersistableFactory.getAnnotationDocumentTestData()
            .someMinimalBuilder()
            .id(456L)
            .build();
        var eod = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .annotationDocumentEntity(annotationDocument)
            .media(null)
            .id(123L)
            .transferAttempts(1)
            .build();

        // when
        String result = dataStoreToArmHelper.generateRawFilename(eod);

        // then
        assertEquals("Filenames don't match", "123_456_1", result);
    }

    @Test
    void generateRawFilename_ShouldSucceed_WithCaseDocument() {
        // given
        var caseDocument = PersistableFactory.getCaseDocumentTestData().someMinimalBuilder()
            .id(456L)
            .build();
        var eod = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .caseDocument(caseDocument)
            .media(null)
            .id(123L)
            .transferAttempts(1)
            .build();

        // when
        String result = dataStoreToArmHelper.generateRawFilename(eod);

        // then
        assertEquals("Filenames don't match", "123_456_1", result);
    }

    @Test
    void generateRawFilename_ShouldSucceed_WithTranscriptionDocument() {
        // given
        var transcriptionDocument = PersistableFactory.getTranscriptionDocument()
            .someMinimalBuilder()
            .id(456L)
            .build();
        var eod = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .transcriptionDocumentEntity(transcriptionDocument)
            .media(null)
            .id(123L)
            .transferAttempts(1)
            .build();

        // when
        String result = dataStoreToArmHelper.generateRawFilename(eod);

        // then
        assertEquals("Filenames don't match", "123_456_1", result);
    }

    @Test
    void copyUnstructuredRawDataToArm_ShouldSucceed() {
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
    void copyUnstructuredRawDataToArm_ShouldReturnFalse_WhenRuntimeExceptionOccurs() {
        // given
        ExternalObjectDirectoryEntity unstructuredExternalObjectDirectory = mock(ExternalObjectDirectoryEntity.class);
        ExternalObjectDirectoryEntity armExternalObjectDirectory = mock(ExternalObjectDirectoryEntity.class);
        String filename = "testfile";
        ObjectRecordStatusEntity previousStatus = eodHelperMocks.getArmIngestionStatus();
        UserAccountEntity userAccount = mock(UserAccountEntity.class);

        doThrow(new RuntimeException("Simulated exception"))
            .when(armDataManagementApi).copyBlobDataToArm(any(), any());

        // when
        boolean result = dataStoreToArmHelper.copyUnstructuredRawDataToArm(
            unstructuredExternalObjectDirectory, armExternalObjectDirectory, filename, previousStatus, userAccount);

        // then
        assertFalse(result);
    }

    @Test
    void copyUnstructuredRawDataToArm_ShouldThrowException_WhenInterruptedExceptionOccurs() {
        // given
        ExternalObjectDirectoryEntity unstructuredExternalObjectDirectory = mock(ExternalObjectDirectoryEntity.class);
        ExternalObjectDirectoryEntity armExternalObjectDirectory = mock(ExternalObjectDirectoryEntity.class);
        String filename = "testfile";
        ObjectRecordStatusEntity previousStatus = eodHelperMocks.getArmIngestionStatus();
        UserAccountEntity userAccount = mock(UserAccountEntity.class);

        doAnswer(invocation -> {
            throw new InterruptedException("Simulated interruption");
        }).when(armDataManagementApi).copyBlobDataToArm(any(), any());

        // when
        assertThrows(InterruptedException.class, () -> dataStoreToArmHelper.copyUnstructuredRawDataToArm(
            unstructuredExternalObjectDirectory, armExternalObjectDirectory, filename, previousStatus, userAccount));
    }

    @Test
    void shouldPushRawDataToArm_ShouldReturnTrue_WhenPreviousStatusIsArmIngestion() {
        // given
        ArmBatchItem batchItem = new ArmBatchItem();
        batchItem.setPreviousStatus(eodHelperMocks.getArmIngestionStatus());
        batchItem.setArmEod(externalObjectDirectoryEntity);
        UserAccountEntity userAccount = mock(UserAccountEntity.class);

        // when
        boolean result = dataStoreToArmHelper.shouldPushRawDataToArm(batchItem, "123_456_1", userAccount);

        // then
        assertThat(result).isTrue();
        verify(armDataManagementApi, never()).listSubmissionBlobs(anyString());
        verify(externalObjectDirectoryRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldPushRawDataToArm_ShouldReturnTrue_WhenPreviousStatusIsArmRawDataFailed() {
        // given
        ArmBatchItem batchItem = new ArmBatchItem();
        batchItem.setPreviousStatus(eodHelperMocks.getFailedArmRawDataStatus());
        UserAccountEntity userAccount = mock(UserAccountEntity.class);

        // when
        boolean result = dataStoreToArmHelper.shouldPushRawDataToArm(batchItem, "123_456_1", userAccount);

        // then
        assertThat(result).isTrue();
        verify(armDataManagementApi, never()).listSubmissionBlobs(anyString());
        verify(externalObjectDirectoryRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldPushRawDataToArm_ShouldReturnFalseAndUpdateToPushed_WhenSubmissionBlobExists() {
        // given
        ArmBatchItem batchItem = createArmBatchItemWithRetryCheckStatus();
        UserAccountEntity userAccount = createUserAccount();
        ArchiveRecord archiveRecord = mock(ArchiveRecord.class);
        String rawFilename = "123_456_1";

        when(armDataManagementApi.listSubmissionBlobs("123_")).thenReturn(List.of("123_456_1"));
        when(archiveRecordService.generateArchiveRecordInfo(123L, rawFilename)).thenReturn(archiveRecord);

        // when
        boolean result = dataStoreToArmHelper.shouldPushRawDataToArm(batchItem, rawFilename, userAccount);

        // then
        assertThat(result).isFalse();
        assertThat(batchItem.getRawFilePushSuccessful()).isTrue();
        assertThat(batchItem.getArchiveRecord()).isSameAs(archiveRecord);
        assertThat(batchItem.getArmEod().getStatus().getId()).isEqualTo(eodHelperMocks.getArmRawDataPushedStatus().getId());
        assertThat(batchItem.getArmEod().getLastModifiedById()).isEqualTo(userAccount.getId());
        assertThat(batchItem.getArmEod().getManifestFile()).isEqualTo("manifest-file.a360");
        verify(armDataManagementApi).listSubmissionBlobs("123_");
        verify(archiveRecordService).generateArchiveRecordInfo(123L, rawFilename);
        verify(externalObjectDirectoryRepository, times(2)).saveAndFlush(batchItem.getArmEod());
    }

    @Test
    void shouldPushRawDataToArm_ShouldReturnTrueAndUpdateToFailed_WhenSubmissionBlobDoesNotExist() {
        // given
        ArmBatchItem batchItem = createArmBatchItemWithRetryCheckStatus();
        UserAccountEntity userAccount = createUserAccount();

        when(armDataManagementApi.listSubmissionBlobs("123_")).thenReturn(List.of());

        // when
        boolean result = dataStoreToArmHelper.shouldPushRawDataToArm(batchItem, "123_456_1", userAccount);

        // then
        assertThat(result).isTrue();
        assertThat(batchItem.getRawFilePushSuccessful()).isFalse();
        assertThat(batchItem.getArchiveRecord()).isNull();
        assertThat(batchItem.getArmEod().getStatus().getId()).isEqualTo(eodHelperMocks.getFailedArmRawDataStatus().getId());
        assertThat(batchItem.getArmEod().getLastModifiedById()).isEqualTo(userAccount.getId());
        assertThat(batchItem.getArmEod().getManifestFile()).isNull();
        verify(armDataManagementApi).listSubmissionBlobs("123_");
        verify(archiveRecordService, never()).generateArchiveRecordInfo(any(), anyString());
        verify(externalObjectDirectoryRepository).saveAndFlush(batchItem.getArmEod());
    }

    @Test
    void shouldPushRawDataToArm_ShouldReturnTrueAndUpdateToFailed_WhenSubmissionBlobListIsNull() {
        // given
        ArmBatchItem batchItem = createArmBatchItemWithRetryCheckStatus();
        UserAccountEntity userAccount = createUserAccount();

        when(armDataManagementApi.listSubmissionBlobs("123_")).thenReturn(null);

        // when
        boolean result = dataStoreToArmHelper.shouldPushRawDataToArm(batchItem, "123_456_1", userAccount);

        // then
        assertThat(result).isTrue();
        assertThat(batchItem.getRawFilePushSuccessful()).isFalse();
        assertThat(batchItem.getArchiveRecord()).isNull();
        assertThat(batchItem.getArmEod().getStatus().getId()).isEqualTo(eodHelperMocks.getFailedArmRawDataStatus().getId());
        assertThat(batchItem.getArmEod().getLastModifiedById()).isEqualTo(userAccount.getId());
        assertThat(batchItem.getArmEod().getManifestFile()).isNull();
        verify(armDataManagementApi).listSubmissionBlobs("123_");
        verify(archiveRecordService, never()).generateArchiveRecordInfo(any(), anyString());
        verify(externalObjectDirectoryRepository).saveAndFlush(batchItem.getArmEod());
    }

    @Test
    void updateExternalObjectDirectoryFailedTransferAttempts_ShouldSucceed() {
        // given
        UserAccountEntity userAccount = mock(UserAccountEntity.class);

        // when
        dataStoreToArmHelper.updateExternalObjectDirectoryFailedTransferAttempts(externalObjectDirectoryEntity, userAccount);

        // then
        verify(externalObjectDirectoryRepository).saveAndFlush(externalObjectDirectoryEntity);
    }

    @Test
    void writeManifestFile_ShouldSucceed() {
        // given
        ArmBatchItems batchItems = mock(ArmBatchItems.class);
        ArchiveRecord archiveRecord = mock(ArchiveRecord.class);
        List<ArchiveRecord> archiveRecords = List.of(archiveRecord);
        when(batchItems.getArchiveRecords()).thenReturn(archiveRecords);

        // when
        dataStoreToArmHelper.generateManifestFileContents(batchItems, "fileName");

        // then
        verify(archiveRecordFileGenerator).generateArchiveRecords("fileName", archiveRecords);
    }

    @Test
    void getArchiveRecordsFileName__ShouldSucceed() {
        when(armDataManagementConfiguration.getFileExtension()).thenReturn("a360");
        String name = dataStoreToArmHelper.getArchiveRecordsFileName("DARTS");

        assertThat(name).matches("DARTS_.+\\.a360");
    }

    private ArmBatchItem createArmBatchItemWithRetryCheckStatus() {
        ObjectRecordStatusEntity retryCheckStatus = new ObjectRecordStatusEntity();
        retryCheckStatus.setId(999);
        retryCheckStatus.setDescription("Retry Check");

        externalObjectDirectoryEntity.setStatus(retryCheckStatus);
        externalObjectDirectoryEntity.setManifestFile("manifest-file.a360");

        ArmBatchItem batchItem = new ArmBatchItem();
        batchItem.setArmEod(externalObjectDirectoryEntity);
        return batchItem;
    }

    private UserAccountEntity createUserAccount() {
        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(789);
        return userAccount;
    }

}