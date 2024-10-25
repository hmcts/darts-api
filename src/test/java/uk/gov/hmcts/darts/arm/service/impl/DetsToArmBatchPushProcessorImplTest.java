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
import uk.gov.hmcts.darts.common.entity.ObjectStateRecordEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectStateRecordRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.test.common.FileStore;
import uk.gov.hmcts.darts.testutils.ExternalObjectDirectoryTestData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DetsToArmBatchPushProcessorImplTest {

    private static final Integer MAX_RETRY_ATTEMPTS = 3;
    private static final UUID DETS_UUID = UUID.randomUUID();
    public static final long OSR_UUID = 987L;

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ExternalLocationTypeRepository externalLocationTypeRepository;
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

    private ExternalObjectDirectoryEntity externalObjectDirectoryEntityDets;

    private ExternalObjectDirectoryEntity externalObjectDirectoryEntityArm;
    @Mock
    private FileOperationService fileOperationService;
    @Mock
    private ArchiveRecordService archiveRecordService;
    @Mock
    private LogApi logApi;

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

    private File manifestFile;
    private MediaEntity mediaEntity1;
    private MediaEntity mediaEntity2;

    private static final EodHelperMocks EOD_HELPER_MOCKS = new EodHelperMocks();

    @AfterAll
    static void close() {
        EOD_HELPER_MOCKS.close();
    }

    @BeforeEach
    void setUp() throws IOException {

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

        lenient().when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(MAX_RETRY_ATTEMPTS);

        lenient().when(detsToArmProcessorConfiguration.getMaxArmManifestItems()).thenReturn(5);
        String fileLocation = tempDirectory.getAbsolutePath();
        lenient().when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        lenient().when(armDataManagementConfiguration.getManifestFilePrefix()).thenReturn("DETS");
        lenient().when(armDataManagementConfiguration.getFileExtension()).thenReturn("a360");

        mediaEntity1 = new MediaEntity();
        externalObjectDirectoryEntityDets = new ExternalObjectDirectoryTestData().createExternalObjectDirectory(
            mediaEntity1,
            ExternalLocationTypeEnum.DETS,
            ObjectRecordStatusEnum.STORED,
            DETS_UUID);
        externalObjectDirectoryEntityDets.setOsrUuid(OSR_UUID);

        mediaEntity2 = new MediaEntity();
        externalObjectDirectoryEntityArm = new ExternalObjectDirectoryTestData().createExternalObjectDirectory(
            mediaEntity2,
            ExternalLocationTypeEnum.ARM,
            ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED,
            UUID.randomUUID());

        externalObjectDirectoryEntityArm.setId(345);

        String filename = String.format("DETS_%s.a360", DETS_UUID);
        manifestFile = new File(fileLocation, filename);
        String content = "Test data";
        try (BufferedWriter fileWriter = Files.newBufferedWriter(manifestFile.toPath());
             PrintWriter printWriter = new PrintWriter(fileWriter)) {
            printWriter.write(content);
        }
        externalObjectDirectoryEntityArm.setManifestFile(manifestFile.getName());

        lenient().when(fileOperationService.createFile(any(), any(), anyBoolean())).thenReturn(manifestFile.toPath());
        lenient().when(detsToArmProcessorConfiguration.getManifestFilePrefix()).thenReturn("DETS");
    }

    @AfterEach
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public void clean() throws Exception {
        FileStore.getFileStore().remove();

        try (var filesStream = Files.list(tempDirectory.toPath())) {
            Assertions.assertEquals(0, filesStream.count());
        }
    }

    @Test
    void processMovingDataFromDetsStorageToArm() throws IOException {
        // given
        ArchiveRecordFileInfo archiveRecordFileInfo = ArchiveRecordFileInfo.builder()
            .fileGenerationSuccessful(true)
            .archiveRecordFile(manifestFile)
            .build();
        when(archiveRecordService.generateArchiveRecord(any(), any())).thenReturn(archiveRecordFileInfo);

        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityDets));
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(
            EodHelper.storedStatus(),
            EodHelper.detsLocation(),
            EodHelper.armLocation(), 5
        )).thenReturn(inboundList);

        when(objectStateRecordRepository.findById(OSR_UUID)).thenReturn(Optional.of(createObjectStateRecordEntity()));
        when(externalObjectDirectoryRepository.saveAndFlush(any())).thenReturn(externalObjectDirectoryEntityArm);
        externalObjectDirectoryEntityArm.setStatus(EodHelper.armIngestionStatus());

        // when
        detsToArmBatchPushProcessor.processDetsToArm(5);

        // then
        verify(externalObjectDirectoryRepository, times(3)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());
        verify(armDataManagementApi).copyBlobDataToArm(eq(DETS_UUID.toString()), any());

        verifyNoMoreInteractions(logApi);
    }

    @Test
    void processMovingDataFromDetsStorageToArmThrowsException() throws IOException {
        // given
        ArchiveRecordFileInfo archiveRecordFileInfo = ArchiveRecordFileInfo.builder()
            .fileGenerationSuccessful(true)
            .archiveRecordFile(manifestFile)
            .build();
        when(archiveRecordService.generateArchiveRecord(any(), any())).thenReturn(archiveRecordFileInfo);

        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityDets));
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(
            EodHelper.storedStatus(),
            EodHelper.detsLocation(),
            EodHelper.armLocation(),
            5
        )).thenReturn(inboundList);

        doReturn(EodHelper.armLocation()).when(externalObjectDirectoryEntityDets).getExternalLocationType();

        NullPointerException genericException = new NullPointerException();
        doThrow(genericException).when(armDataManagementApi).copyBlobDataToArm(any(), any());

        // when
        detsToArmBatchPushProcessor.processDetsToArm(5);

        // then
        verify(externalObjectDirectoryRepository, times(2)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());
    }

    @Test
    void processMovingDataFromDetsStorageToArmThrowsBlobExceptionWhenSendingManifestFile() throws IOException {
        // given
        String fileLocation = tempDirectory.getAbsolutePath();
        String filename = String.format("DETS_%s.a360", DETS_UUID);
        File archiveRecordFile = new File(fileLocation, filename);
        String content = "Test data";
        FileStore.getFileStore().create(archiveRecordFile.toPath());
        try (BufferedWriter fileWriter = Files.newBufferedWriter(archiveRecordFile.toPath()); PrintWriter printWriter = new PrintWriter(fileWriter)) {
            printWriter.write(content);
        }

        ArchiveRecordFileInfo archiveRecordFileInfo = ArchiveRecordFileInfo.builder()
            .fileGenerationSuccessful(true)
            .archiveRecordFile(archiveRecordFile)
            .build();
        when(archiveRecordService.generateArchiveRecord(any(), any())).thenReturn(archiveRecordFileInfo);

        List<ExternalObjectDirectoryEntity> pendingUnstructuredStorageItems = new ArrayList<>(Collections.emptyList());
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(
            EodHelper.storedStatus(),
            EodHelper.detsLocation(),
            EodHelper.armLocation(),
            4
        )).thenReturn(pendingUnstructuredStorageItems);

        List<ObjectRecordStatusEntity> failedStatusesList = new ArrayList<>();
        failedStatusesList.add(EodHelper.failedArmRawDataStatus());
        failedStatusesList.add(EodHelper.failedArmManifestFileStatus());

        List<ExternalObjectDirectoryEntity> pendingFailureList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityArm));
        when(externalObjectDirectoryRepository.findNotFinishedAndNotExceededRetryInStorageLocationForDets(
            failedStatusesList,
            externalLocationTypeRepository.getReferenceById(3),
            armDataManagementConfiguration.getMaxRetryAttempts(), Pageable.ofSize(5)
        )).thenReturn(pendingFailureList);

        when(externalObjectDirectoryEntityArm.getAnnotationDocumentEntity()).thenReturn(annotationDocumentEntity);
        when(externalObjectDirectoryRepository.findMatchingExternalObjectDirectoryEntityByLocation(
            EodHelper.storedStatus(),
            EodHelper.detsLocation(),
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

        // when
        detsToArmBatchPushProcessor.processDetsToArm(5);

        // then
        verify(externalObjectDirectoryRepository).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        verify(logApi).armPushSuccessful(anyInt());
    }

    @Test
    void processMovingDataFromDetsStorageToArmThrowsGenericExceptionWhenSendingManifestFile() throws IOException {
        // given
        String fileLocation = tempDirectory.getAbsolutePath();
        File archiveRecordFile = new File(fileLocation, "1_1_1.a360");

        FileStore.getFileStore().create(archiveRecordFile.toPath());
        ArchiveRecordFileInfo archiveRecordFileInfo = ArchiveRecordFileInfo.builder()
            .fileGenerationSuccessful(true)
            .archiveRecordFile(archiveRecordFile)
            .build();
        when(archiveRecordService.generateArchiveRecord(any(), any())).thenReturn(archiveRecordFileInfo);

        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityDets));
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(
            EodHelper.storedStatus(),
            EodHelper.detsLocation(),
            EodHelper.armLocation(),
            5
        )).thenReturn(inboundList);

        doReturn(EodHelper.armLocation()).when(externalObjectDirectoryEntityDets).getExternalLocationType();

        NullPointerException genericException = new NullPointerException();
        when(armDataManagementApi.saveBlobDataToArm(any(), any())).thenThrow(genericException);

        // when
        detsToArmBatchPushProcessor.processDetsToArm(5);

        // then
        verify(externalObjectDirectoryRepository, times(3)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        verifyNoMoreInteractions(logApi);
    }

    @Test
    void processMovingDataFromDetsStorageToArmThrowsExceptionWhenSendingManifestFile() throws IOException {
        // given
        String fileLocation = tempDirectory.getAbsolutePath();
        File archiveRecordFile = new File(fileLocation, "1_1_1.a360");
        FileStore.getFileStore().create(archiveRecordFile.toPath());
        ArchiveRecordFileInfo archiveRecordFileInfo = ArchiveRecordFileInfo.builder()
            .fileGenerationSuccessful(true)
            .archiveRecordFile(archiveRecordFile)
            .build();
        when(archiveRecordService.generateArchiveRecord(any(), any())).thenReturn(archiveRecordFileInfo);

        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityDets));
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(
            EodHelper.storedStatus(),
            EodHelper.detsLocation(),
            EodHelper.armLocation(),
            5
        )).thenReturn(inboundList);

        doReturn(EodHelper.armLocation()).when(externalObjectDirectoryEntityDets).getExternalLocationType();

        NullPointerException genericException = new NullPointerException();
        when(armDataManagementApi.saveBlobDataToArm(any(), any())).thenThrow(genericException);

        // when
        detsToArmBatchPushProcessor.processDetsToArm(5);

        // then
        verify(externalObjectDirectoryRepository, times(3)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        verifyNoMoreInteractions(logApi);
    }

    @Test
    void processPreviousFailedAttemptMovingFromDetsStorageToArm() {
        // given
        String fileLocation = tempDirectory.getAbsolutePath();
        ArchiveRecordFileInfo archiveRecordFileInfo = ArchiveRecordFileInfo.builder()
            .fileGenerationSuccessful(true)
            .archiveRecordFile(new File(fileLocation, "1_1_1.a360"))
            .build();
        when(archiveRecordService.generateArchiveRecord(any(), any())).thenReturn(archiveRecordFileInfo);

        List<ExternalObjectDirectoryEntity> pendingUnstructuredStorageItems = new ArrayList<>(Collections.emptyList());
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(
            EodHelper.storedStatus(),
            EodHelper.detsLocation(),
            EodHelper.armLocation(),
            4
        )).thenReturn(pendingUnstructuredStorageItems);

        List<ObjectRecordStatusEntity> failedStatusesList = new ArrayList<>();
        failedStatusesList.add(EodHelper.failedArmRawDataStatus());
        failedStatusesList.add(EodHelper.failedArmManifestFileStatus());

        List<ExternalObjectDirectoryEntity> pendingFailureList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityArm));
        when(externalObjectDirectoryRepository.findNotFinishedAndNotExceededRetryInStorageLocationForDets(
            failedStatusesList,
            externalLocationTypeRepository.getReferenceById(3),
            armDataManagementConfiguration.getMaxRetryAttempts(), Pageable.ofSize(5)
        )).thenReturn(pendingFailureList);

        doReturn(EodHelper.armLocation()).when(externalObjectDirectoryEntityArm).getExternalLocationType();
        when(externalObjectDirectoryEntityArm.getMedia()).thenReturn(mediaEntity);
        doReturn(EodHelper.failedArmRawDataStatus()).when(externalObjectDirectoryEntityArm).getStatus();

        when(externalObjectDirectoryRepository.findMatchingExternalObjectDirectoryEntityByLocation(
            EodHelper.storedStatus(),
            EodHelper.detsLocation(),
            externalObjectDirectoryEntityArm.getMedia(),
            externalObjectDirectoryEntityArm.getTranscriptionDocumentEntity(),
            externalObjectDirectoryEntityArm.getAnnotationDocumentEntity(),
            externalObjectDirectoryEntityArm.getCaseDocument()
        )).thenReturn(Optional.of(externalObjectDirectoryEntityDets));

        // when
        detsToArmBatchPushProcessor.processDetsToArm(5);

        // then
        verify(externalObjectDirectoryRepository).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        verifyNoMoreInteractions(logApi);
    }

    @Test
    void processPreviousFailedAttempt() {
        // given
        List<ExternalObjectDirectoryEntity> pendingUnstructuredStorageItems = new ArrayList<>(Collections.emptyList());

        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(
            EodHelper.storedStatus(),
            EodHelper.detsLocation(),
            EodHelper.armLocation(),
            4
        )).thenReturn(pendingUnstructuredStorageItems);

        List<ObjectRecordStatusEntity> failedStatusesList = new ArrayList<>();
        failedStatusesList.add(EodHelper.failedArmRawDataStatus());
        failedStatusesList.add(EodHelper.failedArmManifestFileStatus());

        List<ExternalObjectDirectoryEntity> pendingFailureList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityArm));
        when(externalObjectDirectoryRepository.findNotFinishedAndNotExceededRetryInStorageLocationForDets(
            failedStatusesList,
            externalLocationTypeRepository.getReferenceById(3),
            armDataManagementConfiguration.getMaxRetryAttempts(), Pageable.ofSize(5)
        )).thenReturn(pendingFailureList);

        doReturn(EodHelper.armLocation()).when(externalObjectDirectoryEntityArm).getExternalLocationType();
        when(externalObjectDirectoryEntityArm.getMedia()).thenReturn(mediaEntity);
        when(externalObjectDirectoryEntityArm.getTranscriptionDocumentEntity()).thenReturn(transcriptionDocumentEntity);
        when(externalObjectDirectoryEntityArm.getAnnotationDocumentEntity()).thenReturn(annotationDocumentEntity);
        doReturn(EodHelper.failedArmRawDataStatus()).when(externalObjectDirectoryEntityArm).getStatus();
        when(externalObjectDirectoryRepository.findMatchingExternalObjectDirectoryEntityByLocation(
            EodHelper.storedStatus(),
            EodHelper.detsLocation(),
            externalObjectDirectoryEntityArm.getMedia(),
            externalObjectDirectoryEntityArm.getTranscriptionDocumentEntity(),
            externalObjectDirectoryEntityArm.getAnnotationDocumentEntity(),
            externalObjectDirectoryEntityArm.getCaseDocument()
        )).thenReturn(Optional.empty());

        // when
        detsToArmBatchPushProcessor.processDetsToArm(5);

        // then
        verify(externalObjectDirectoryRepository, times(1)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        verifyNoMoreInteractions(logApi);
    }

    private ObjectStateRecordEntity createObjectStateRecordEntity() {
        ObjectStateRecordEntity objectStateRecordEntity = new ObjectStateRecordEntity();
        objectStateRecordEntity.setUuid(OSR_UUID);
        objectStateRecordEntity.setEodId(DETS_UUID.toString());
        return objectStateRecordEntity;
    }

    @Test
    void testDartsArmClientConfigInBatchQuery() {
        ExternalObjectDirectoryEntity eod10 = new ExternalObjectDirectoryEntity();
        eod10.setId(10);
        eod10.setExternalLocationType(EodHelper.armLocation());
        eod10.setStatus(EodHelper.failedArmManifestFileStatus());
        //given
        when(externalObjectDirectoryRepository.findNotFinishedAndNotExceededRetryInStorageLocation(any(), any(), any(), any())).thenReturn(List.of(eod10));
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(any(), any(), any(), any())).thenReturn(emptyList());
        EOD_HELPER_MOCKS.givenIsEqualLocationReturns(true);
        when(detsToArmProcessorConfiguration.getMaxArmManifestItems()).thenReturn(10);
        when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(3);

        //when
        detsToArmBatchPushProcessor.processDetsToArm(200);

        //then
        verify(externalObjectDirectoryRepository).findEodsNotInOtherStorage(
            EodHelper.storedStatus(),
            EodHelper.unstructuredLocation(),
            EodHelper.armLocation(), 199
        );

        verify(logApi).armPushFailed(anyInt());
    }

    @Test
    void testPaginatedBatchQuery() throws IOException {
        //given
        when(externalObjectDirectoryRepository.findNotFinishedAndNotExceededRetryInStorageLocation(any(), any(), any(), any())).thenReturn(
            List.of(externalObjectDirectoryEntityDets));
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(any(), any(), any(), any())).thenReturn(emptyList());
        when(detsToArmProcessorConfiguration.getMaxArmManifestItems()).thenReturn(100);
        when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(3);

        when(fileOperationService.createFile(any(), any(), anyBoolean())).thenReturn(manifestFile.toPath());

        //when
        detsToArmBatchPushProcessor.processDetsToArm(5000);

        //then
        verify(externalObjectDirectoryRepository).findNotFinishedAndNotExceededRetryInStorageLocation(
            any(),
            any(ExternalLocationTypeEntity.class),
            eq(3),
            eq(Pageable.ofSize(5000)));

        verify(externalObjectDirectoryRepository).findEodsNotInOtherStorage(
            EodHelper.storedStatus(),
            EodHelper.unstructuredLocation(),
            EodHelper.armLocation(), 4998
        );

        verifyNoMoreInteractions(logApi);
    }

    @Test
    void testManifestFileName() throws IOException {
        //given
        when(detsToArmProcessorConfiguration.getManifestFilePrefix()).thenReturn("DETS");
        when(armDataManagementConfiguration.getFileExtension()).thenReturn("a360");
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn("/temp_workspace");
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(any(), any(), any(), any())).thenReturn(List.of(externalObjectDirectoryEntityArm));
        when(detsToArmProcessorConfiguration.getMaxArmManifestItems()).thenReturn(1000);
        when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(3);

        //when
        detsToArmBatchPushProcessor.processDetsToArm(5);

        //then
        verify(fileOperationService).createFile(matches("DETS_.+\\.a360"), eq("/temp_workspace"), eq(true));

        verifyNoMoreInteractions(logApi);
    }


    public ObjectStateRecordEntity createObjectStateRecordEntity(Long uuid) {
        ObjectStateRecordEntity objectStateRecordEntity = new ObjectStateRecordEntity();
        objectStateRecordEntity.setUuid(uuid);
        objectStateRecordEntity.setEodId("EOD123");
        objectStateRecordEntity.setArmEodId("ARM123");
        objectStateRecordEntity.setParentId("Parent123");
        objectStateRecordEntity.setParentObjectId("ParentObject123");
        objectStateRecordEntity.setContentObjectId("ContentObject123");
        objectStateRecordEntity.setObjectType("Type123");
        objectStateRecordEntity.setIdClip("Clip123");
        objectStateRecordEntity.setIdCase("Case123");
        objectStateRecordEntity.setCourthouseName("Courthouse123");
        objectStateRecordEntity.setCasId(123);
        objectStateRecordEntity.setDateLastAccessed(OffsetDateTime.now());
        objectStateRecordEntity.setRelationId("Relation123");
        objectStateRecordEntity.setDetsLocation("DetsLocation123");
        objectStateRecordEntity.setFlagFileTransferToDets(true);
        objectStateRecordEntity.setDateFileTransferToDets(OffsetDateTime.now());
        objectStateRecordEntity.setMd5DocTransferToDets("MD5123");
        objectStateRecordEntity.setFileSizeBytesCentera(123L);
        objectStateRecordEntity.setFileSizeBytesDets(123L);
        objectStateRecordEntity.setFlagFileAvScanPass(true);
        objectStateRecordEntity.setDateFileAvScanPass(OffsetDateTime.now());
        objectStateRecordEntity.setFlagFileTransfToarml(true);
        objectStateRecordEntity.setDateFileTransfToarml(OffsetDateTime.now());
        objectStateRecordEntity.setMd5FileTransfArml("MD5ARML123");
        objectStateRecordEntity.setFileSizeBytesArml(123L);
        objectStateRecordEntity.setFlagFileMfstCreated(true);
        objectStateRecordEntity.setDateFileMfstCreated(OffsetDateTime.now());
        objectStateRecordEntity.setIdManifestFile("Manifest123");
        objectStateRecordEntity.setFlagMfstTransfToArml(true);
        objectStateRecordEntity.setDateMfstTransfToArml(OffsetDateTime.now());
        objectStateRecordEntity.setFlagRspnRecvdFromArml(true);
        objectStateRecordEntity.setDateRspnRecvdFromArml(OffsetDateTime.now());
        objectStateRecordEntity.setFlagFileIngestStatus(true);
        objectStateRecordEntity.setDateFileIngestToArm(OffsetDateTime.now());
        objectStateRecordEntity.setMd5FileIngestToArm("MD5Ingest123");
        objectStateRecordEntity.setFileSizeIngestToArm(123L);
        objectStateRecordEntity.setIdResponseFile("ResponseFile123");
        objectStateRecordEntity.setIdResponseCrFile("ResponseCrFile123");
        objectStateRecordEntity.setIdResponseUfFile("ResponseUfFile123");
        objectStateRecordEntity.setFlagFileDetsCleanupStatus(true);
        objectStateRecordEntity.setDateFileDetsCleanup(OffsetDateTime.now());
        objectStateRecordEntity.setFlagFileRetainedInOds(true);
        objectStateRecordEntity.setObjectStatus("Status123");
        return objectStateRecordEntity;
    }
}