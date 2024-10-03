package uk.gov.hmcts.darts.arm.service.impl;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobStorageException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.config.DetsToArmProcessorConfiguration;
import uk.gov.hmcts.darts.arm.helper.DataStoreToArmHelper;
import uk.gov.hmcts.darts.arm.model.record.ArchiveRecordFileInfo;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
import uk.gov.hmcts.darts.arm.service.DetsToArmBatchPushProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.ObjectStateRecordRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.test.common.FileStore;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED;

@ExtendWith(MockitoExtension.class)
class DetsToArmBatchPushProcessorImplTest {

    private static final Integer MAX_RETRY_ATTEMPTS = 3;
    private static final UUID DETS_UUID = UUID.randomUUID();

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ObjectRecordStatusRepository objectRecordStatusRepository;
    @Mock
    private ExternalLocationTypeRepository externalLocationTypeRepository;
    @Mock
    private DataManagementApi dataManagementApi;
    @Mock
    private ArmDataManagementApi armDataManagementApi;
    @Mock
    private UserIdentity userIdentity;
    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @Mock
    private MediaEntity mediaEntity;
    @Mock
    private TranscriptionDocumentEntity transcriptionDocumentEntity;
    @Mock
    private AnnotationDocumentEntity annotationDocumentEntity;
    @Mock
    private ExternalObjectDirectoryEntity externalObjectDirectoryEntityDets;
    @Mock
    private ExternalObjectDirectoryEntity externalObjectDirectoryEntityArm;
    @Mock
    private ExternalLocationTypeEntity externalLocationTypeDets;
    @Mock
    private ExternalLocationTypeEntity externalLocationTypeArm;
    @Mock
    private FileOperationService fileOperationService;
    @Mock
    private ArchiveRecordService archiveRecordService;
    @Mock
    private LogApi logApi;

    @Mock
    private ObjectRecordStatusEntity objectRecordStatusEntityStored;
    @Mock
    private ObjectRecordStatusEntity objectRecordStatusEntityArmIngestion;
    @Mock
    private ObjectRecordStatusEntity objectRecordStatusEntityRawDataFailed;
    @Mock
    private ObjectRecordStatusEntity objectRecordStatusEntityManifestFailed;
    @Mock
    private ObjectRecordStatusEntity objectRecordStatusEntityArmDropZone;
    @Mock
    private DetsToArmProcessorConfiguration detsToArmProcessorConfiguration;
    @Mock
    private ObjectStateRecordRepository objectStateRecordRepository;
    @Mock
    private CurrentTimeHelper currentTimeHelper;
    @InjectMocks
    private DataStoreToArmHelper dataStoreToArmHelper;
    @Captor
    private ArgumentCaptor<ExternalObjectDirectoryEntity> externalObjectDirectoryEntityCaptor;

    private DetsToArmBatchPushProcessor detsToArmBatchPushProcessor;


    @TempDir
    private File tempDirectory;

    private static final EodHelperMocks EOD_HELPER_MOCKS = new EodHelperMocks();

    @AfterAll
    static void close() {
        EOD_HELPER_MOCKS.close();
    }

    @BeforeEach
    void setUp() {

        detsToArmBatchPushProcessor = new DetsToArmBatchPushProcessorImpl(
            archiveRecordService,
            dataStoreToArmHelper,
            userIdentity,
            logApi,
            armDataManagementConfiguration,
            externalObjectDirectoryRepository,
            fileOperationService,
            armDataManagementApi,
            detsToArmProcessorConfiguration,
            objectStateRecordRepository,
            currentTimeHelper
        );
        lenient().when(objectRecordStatusRepository.findById(2)).thenReturn(Optional.of(objectRecordStatusEntityStored));
        lenient().when(objectRecordStatusRepository.findById(12)).thenReturn(Optional.of(objectRecordStatusEntityArmIngestion));
        lenient().when(objectRecordStatusRepository.findById(14)).thenReturn(Optional.of(objectRecordStatusEntityRawDataFailed));
        lenient().when(objectRecordStatusRepository.findById(15)).thenReturn(Optional.of(objectRecordStatusEntityManifestFailed));
        lenient().when(objectRecordStatusRepository.findById(13)).thenReturn(Optional.of(objectRecordStatusEntityArmDropZone));
        lenient().when(externalLocationTypeDets.getId()).thenReturn(ExternalLocationTypeEnum.DETS.getId());
        lenient().when(objectRecordStatusRepository.getReferenceById(ARM_RAW_DATA_FAILED.getId())).thenReturn(objectRecordStatusEntityRawDataFailed);
        lenient().when(objectRecordStatusRepository.getReferenceById(ARM_MANIFEST_FAILED.getId())).thenReturn(objectRecordStatusEntityManifestFailed);
        lenient().when(objectRecordStatusEntityRawDataFailed.getDescription()).thenReturn(ARM_RAW_DATA_FAILED.name());

        lenient().when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(MAX_RETRY_ATTEMPTS);

        lenient().when(externalObjectDirectoryEntityArm.getId()).thenReturn(123);
        lenient().when(externalObjectDirectoryEntityDets.getExternalLocation()).thenReturn(DETS_UUID);
        lenient().when(detsToArmProcessorConfiguration.getMaxArmManifestItems()).thenReturn(5);
        lenient().when(EodHelper.failedArmRawDataStatus()).thenReturn(objectRecordStatusEntityRawDataFailed);

    }

    @AfterEach
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public void clean() throws Exception {
        FileStore.getFileStore().remove();
        Assertions.assertEquals(0, Files.list(tempDirectory.toPath()).count());
    }

    @Test
    void processMovingDataFromUnstructuredStorageToArm() {

        String fileLocation = tempDirectory.getAbsolutePath();
        ArchiveRecordFileInfo archiveRecordFileInfo = ArchiveRecordFileInfo.builder()
            .fileGenerationSuccessful(true)
            .archiveRecordFile(new File(fileLocation, "1_1_1.a360"))
            .build();
        when(archiveRecordService.generateArchiveRecord(any(), any())).thenReturn(archiveRecordFileInfo);

        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeDets);
        when(externalLocationTypeRepository.getReferenceById(3)).thenReturn(externalLocationTypeArm);

        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityDets));
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(
            EodHelper.storedStatus(),
            externalLocationTypeDets,
            EodHelper.armLocation(), 5
        )).thenReturn(inboundList);

        when(externalObjectDirectoryEntityDets.getExternalLocationType()).thenReturn(externalLocationTypeDets);
        when(externalLocationTypeDets.getId()).thenReturn(ExternalLocationTypeEnum.UNSTRUCTURED.getId());
        when(externalObjectDirectoryEntityDets.getExternalLocationType()).thenReturn(externalLocationTypeDets);
        lenient().when(externalObjectDirectoryEntityDets.getExternalLocation()).thenReturn(DETS_UUID);

        detsToArmBatchPushProcessor.processDetsToArm(5);

        verify(externalObjectDirectoryRepository, times(3)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());
        verify(armDataManagementApi).copyBlobDataToArm(eq(DETS_UUID.toString()), any());

        verifyNoMoreInteractions(logApi);
    }

    @Test
    void processMovingDataFromUnstructuredStorageToArmThrowsException() {

        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeDets);
        when(externalLocationTypeRepository.getReferenceById(3)).thenReturn(externalLocationTypeArm);

        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityDets));
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(
            EodHelper.storedStatus(),
            externalLocationTypeDets,
            EodHelper.armLocation(),
            5
        )).thenReturn(inboundList);

        when(externalLocationTypeDets.getId()).thenReturn(ExternalLocationTypeEnum.UNSTRUCTURED.getId());
        when(externalObjectDirectoryEntityDets.getExternalLocationType()).thenReturn(externalLocationTypeDets);

        NullPointerException genericException = new NullPointerException();
        doThrow(genericException).when(armDataManagementApi).copyBlobDataToArm(any(), any());

        detsToArmBatchPushProcessor.processDetsToArm(5);

        verify(externalObjectDirectoryRepository, times(2)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());
    }

    @Test
    void processMovingDataFromUnstructuredStorageToArmThrowsBlobExceptionWhenSendingManifestFile() throws IOException {

        String fileLocation = tempDirectory.getAbsolutePath();
        File archiveRecordFile = new File(fileLocation, "1_1_1.a360");
        String content = "Test data";
        FileStore.getFileStore().create(archiveRecordFile.toPath());
        try (BufferedWriter fileWriter = Files.newBufferedWriter(archiveRecordFile.toPath());
             PrintWriter printWriter = new PrintWriter(fileWriter)) {
            printWriter.write(content);
        }

        ArchiveRecordFileInfo archiveRecordFileInfo = ArchiveRecordFileInfo.builder()
            .fileGenerationSuccessful(true)
            .archiveRecordFile(archiveRecordFile)
            .build();
        when(archiveRecordService.generateArchiveRecord(any(), any())).thenReturn(archiveRecordFileInfo);

        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeDets);
        when(externalLocationTypeRepository.getReferenceById(3)).thenReturn(externalLocationTypeArm);

        List<ExternalObjectDirectoryEntity> pendingUnstructuredStorageItems = new ArrayList<>(Collections.emptyList());
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(
            EodHelper.storedStatus(),
            externalLocationTypeDets,
            EodHelper.armLocation(),
            4
        )).thenReturn(pendingUnstructuredStorageItems);

        List<ObjectRecordStatusEntity> failedStatusesList = new ArrayList<>();
        failedStatusesList.add(objectRecordStatusEntityRawDataFailed);
        failedStatusesList.add(objectRecordStatusEntityManifestFailed);

        List<ExternalObjectDirectoryEntity> pendingFailureList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityArm));
        when(externalObjectDirectoryRepository.findNotFinishedAndNotExceededRetryInStorageLocationForDets(
            failedStatusesList,
            externalLocationTypeRepository.getReferenceById(3),
            armDataManagementConfiguration.getMaxRetryAttempts(), Pageable.ofSize(5)
        )).thenReturn(pendingFailureList);

        when(externalObjectDirectoryEntityArm.getExternalLocationType()).thenReturn(externalLocationTypeArm);
        when(externalObjectDirectoryEntityArm.getStatus()).thenReturn(objectRecordStatusEntityManifestFailed);
        when(externalObjectDirectoryEntityArm.getMedia()).thenReturn(mediaEntity);
        when(externalObjectDirectoryEntityArm.getAnnotationDocumentEntity()).thenReturn(annotationDocumentEntity);
        when(externalObjectDirectoryRepository.findMatchingExternalObjectDirectoryEntityByLocation(
            EodHelper.storedStatus(),
            externalLocationTypeDets,
            externalObjectDirectoryEntityArm.getMedia(),
            externalObjectDirectoryEntityArm.getTranscriptionDocumentEntity(),
            externalObjectDirectoryEntityArm.getAnnotationDocumentEntity(),
            externalObjectDirectoryEntityArm.getCaseDocument()
        )).thenReturn(Optional.of(externalObjectDirectoryEntityDets));

        BinaryData manifest = BinaryData.fromFile(Path.of(archiveRecordFile.getAbsolutePath()));
        when(fileOperationService.convertFileToBinaryData(any())).thenReturn(manifest);
        BlobStorageException blobStorageException = mock(BlobStorageException.class);
        when(blobStorageException.getStatusCode()).thenReturn(409);
        when(blobStorageException.getMessage()).thenReturn("Copying blob failed");
        when(armDataManagementApi.saveBlobDataToArm("1_1_1.a360", manifest)).thenThrow(blobStorageException);

        detsToArmBatchPushProcessor.processDetsToArm(5);

        verify(externalObjectDirectoryRepository).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        verify(logApi).armPushSuccessful(anyInt());
    }

    @Test
    void processMovingDataFromUnstructuredStorageToArmThrowsGenericExceptionWhenSendingManifestFile() throws IOException {

        String fileLocation = tempDirectory.getAbsolutePath();
        File archiveRecordFile = new File(fileLocation, "1_1_1.a360");

        FileStore.getFileStore().create(archiveRecordFile.toPath());
        ArchiveRecordFileInfo archiveRecordFileInfo = ArchiveRecordFileInfo.builder()
            .fileGenerationSuccessful(true)
            .archiveRecordFile(archiveRecordFile)
            .build();
        when(archiveRecordService.generateArchiveRecord(any(), any())).thenReturn(archiveRecordFileInfo);

        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeDets);
        when(externalLocationTypeRepository.getReferenceById(3)).thenReturn(externalLocationTypeArm);

        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityDets));
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(
            EodHelper.storedStatus(),
            externalLocationTypeDets,
            EodHelper.armLocation(),
            5
        )).thenReturn(inboundList);

        when(externalObjectDirectoryEntityDets.getExternalLocationType()).thenReturn(externalLocationTypeDets);
        when(externalLocationTypeDets.getId()).thenReturn(ExternalLocationTypeEnum.UNSTRUCTURED.getId());
        when(externalObjectDirectoryEntityDets.getExternalLocationType()).thenReturn(externalLocationTypeDets);

        NullPointerException genericException = new NullPointerException();
        when(armDataManagementApi.saveBlobDataToArm(any(), any())).thenThrow(genericException);

        detsToArmBatchPushProcessor.processDetsToArm(5);

        verify(externalObjectDirectoryRepository, times(3)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        verifyNoMoreInteractions(logApi);
    }

    @Test
    void processMovingDataFromUnstructuredStorageToArmThrowsExceptionWhenSendingManifestFile() throws IOException {

        String fileLocation = tempDirectory.getAbsolutePath();
        File archiveRecordFile = new File(fileLocation, "1_1_1.a360");
        FileStore.getFileStore().create(archiveRecordFile.toPath());
        ArchiveRecordFileInfo archiveRecordFileInfo = ArchiveRecordFileInfo.builder()
            .fileGenerationSuccessful(true)
            .archiveRecordFile(archiveRecordFile)
            .build();
        when(archiveRecordService.generateArchiveRecord(any(), any())).thenReturn(archiveRecordFileInfo);

        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeDets);
        when(externalLocationTypeRepository.getReferenceById(3)).thenReturn(externalLocationTypeArm);

        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityDets));
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(
            EodHelper.storedStatus(),
            externalLocationTypeDets,
            EodHelper.armLocation(),
            5
        )).thenReturn(inboundList);

        when(externalObjectDirectoryEntityDets.getExternalLocationType()).thenReturn(externalLocationTypeDets);
        when(externalLocationTypeDets.getId()).thenReturn(ExternalLocationTypeEnum.UNSTRUCTURED.getId());
        when(externalObjectDirectoryEntityDets.getExternalLocationType()).thenReturn(externalLocationTypeDets);

        NullPointerException genericException = new NullPointerException();
        when(armDataManagementApi.saveBlobDataToArm(any(), any())).thenThrow(genericException);

        detsToArmBatchPushProcessor.processDetsToArm(5);

        verify(externalObjectDirectoryRepository, times(3)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        verifyNoMoreInteractions(logApi);
    }

    @Test
    void processPreviousFailedAttemptMovingFromUnstructuredStorageToArm() {
        when(objectRecordStatusEntityRawDataFailed.getDescription()).thenReturn("FAILURE_ARM_RAW_DATA_FAILED");

        String fileLocation = tempDirectory.getAbsolutePath();
        ArchiveRecordFileInfo archiveRecordFileInfo = ArchiveRecordFileInfo.builder()
            .fileGenerationSuccessful(true)
            .archiveRecordFile(new File(fileLocation, "1_1_1.a360"))
            .build();
        when(archiveRecordService.generateArchiveRecord(any(), any())).thenReturn(archiveRecordFileInfo);

        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeDets);
        when(externalLocationTypeRepository.getReferenceById(3)).thenReturn(externalLocationTypeArm);

        List<ExternalObjectDirectoryEntity> pendingUnstructuredStorageItems = new ArrayList<>(Collections.emptyList());
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(
            EodHelper.storedStatus(),
            externalLocationTypeDets,
            EodHelper.armLocation(),
            4
        )).thenReturn(pendingUnstructuredStorageItems);

        List<ObjectRecordStatusEntity> failedStatusesList = new ArrayList<>();
        failedStatusesList.add(objectRecordStatusEntityRawDataFailed);
        failedStatusesList.add(objectRecordStatusEntityManifestFailed);

        List<ExternalObjectDirectoryEntity> pendingFailureList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityArm));
        when(externalObjectDirectoryRepository.findNotFinishedAndNotExceededRetryInStorageLocationForDets(
            failedStatusesList,
            externalLocationTypeRepository.getReferenceById(3),
            armDataManagementConfiguration.getMaxRetryAttempts(), Pageable.ofSize(5)
        )).thenReturn(pendingFailureList);

        when(externalObjectDirectoryEntityArm.getExternalLocationType()).thenReturn(externalLocationTypeArm);
        when(externalObjectDirectoryEntityArm.getMedia()).thenReturn(mediaEntity);
        when(externalObjectDirectoryEntityArm.getTranscriptionDocumentEntity()).thenReturn(transcriptionDocumentEntity);
        when(externalObjectDirectoryEntityArm.getAnnotationDocumentEntity()).thenReturn(annotationDocumentEntity);
        when(externalObjectDirectoryEntityArm.getStatus()).thenReturn(objectRecordStatusEntityRawDataFailed);
        when(externalObjectDirectoryRepository.findMatchingExternalObjectDirectoryEntityByLocation(
            EodHelper.storedStatus(),
            externalLocationTypeDets,
            externalObjectDirectoryEntityArm.getMedia(),
            externalObjectDirectoryEntityArm.getTranscriptionDocumentEntity(),
            externalObjectDirectoryEntityArm.getAnnotationDocumentEntity(),
            externalObjectDirectoryEntityArm.getCaseDocument()
        )).thenReturn(Optional.of(externalObjectDirectoryEntityDets));

        detsToArmBatchPushProcessor.processDetsToArm(5);

        verify(externalObjectDirectoryRepository).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        verifyNoMoreInteractions(logApi);
    }

    @Test
    void processPreviousFailedAttempt() {

        when(objectRecordStatusEntityRawDataFailed.getDescription()).thenReturn("FAILURE_ARM_RAW_DATA_FAILED");

        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeDets);
        when(externalLocationTypeRepository.getReferenceById(3)).thenReturn(externalLocationTypeArm);

        List<ExternalObjectDirectoryEntity> pendingUnstructuredStorageItems = new ArrayList<>(Collections.emptyList());

        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(
            EodHelper.storedStatus(),
            externalLocationTypeDets,
            EodHelper.armLocation(),
            4
        )).thenReturn(pendingUnstructuredStorageItems);

        List<ObjectRecordStatusEntity> failedStatusesList = new ArrayList<>();
        failedStatusesList.add(objectRecordStatusEntityRawDataFailed);
        failedStatusesList.add(objectRecordStatusEntityManifestFailed);

        List<ExternalObjectDirectoryEntity> pendingFailureList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityArm));
        when(externalObjectDirectoryRepository.findNotFinishedAndNotExceededRetryInStorageLocationForDets(
            failedStatusesList,
            externalLocationTypeRepository.getReferenceById(3),
            armDataManagementConfiguration.getMaxRetryAttempts(), Pageable.ofSize(5)
        )).thenReturn(pendingFailureList);

        when(externalObjectDirectoryEntityArm.getExternalLocationType()).thenReturn(externalLocationTypeArm);
        when(externalObjectDirectoryEntityArm.getMedia()).thenReturn(mediaEntity);
        when(externalObjectDirectoryEntityArm.getTranscriptionDocumentEntity()).thenReturn(transcriptionDocumentEntity);
        when(externalObjectDirectoryEntityArm.getAnnotationDocumentEntity()).thenReturn(annotationDocumentEntity);
        when(externalObjectDirectoryEntityArm.getStatus()).thenReturn(objectRecordStatusEntityRawDataFailed);
        when(externalObjectDirectoryRepository.findMatchingExternalObjectDirectoryEntityByLocation(
            EodHelper.storedStatus(),
            externalLocationTypeDets,
            externalObjectDirectoryEntityArm.getMedia(),
            externalObjectDirectoryEntityArm.getTranscriptionDocumentEntity(),
            externalObjectDirectoryEntityArm.getAnnotationDocumentEntity(),
            externalObjectDirectoryEntityArm.getCaseDocument()
        )).thenReturn(Optional.empty());

        detsToArmBatchPushProcessor.processDetsToArm(5);

        verify(externalObjectDirectoryRepository, times(1)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        verifyNoMoreInteractions(logApi);
    }
}