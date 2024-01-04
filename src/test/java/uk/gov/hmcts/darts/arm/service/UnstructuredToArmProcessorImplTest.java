package uk.gov.hmcts.darts.arm.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.model.record.ArchiveRecordFileInfo;
import uk.gov.hmcts.darts.arm.service.impl.UnstructuredToArmProcessorImpl;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnstructuredToArmProcessorImplTest {

    private static final String TEST_BINARY_DATA = "test binary data";
    private static final Integer MAX_RETRY_ATTEMPTS = 3;

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
    private UserAccountRepository userAccountRepository;
    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @Mock
    private MediaEntity mediaEntity;
    @Mock
    private TranscriptionDocumentEntity transcriptionDocumentEntity;
    @Mock
    private AnnotationDocumentEntity annotationDocumentEntity;
    @Mock
    ExternalObjectDirectoryEntity externalObjectDirectoryEntityUnstructured;
    @Mock
    ExternalObjectDirectoryEntity externalObjectDirectoryEntityArm;
    @Mock
    ExternalLocationTypeEntity externalLocationTypeUnstructured;
    @Mock
    ExternalLocationTypeEntity externalLocationTypeArm;
    @Mock
    FileOperationService fileOperationService;
    @Mock
    ArchiveRecordService archiveRecordService;

    private UnstructuredToArmProcessor unstructuredToArmProcessor;

    @Mock
    ObjectRecordStatusEntity objectRecordStatusEntityStored;
    @Mock
    ObjectRecordStatusEntity objectRecordStatusEntityArmIngestion;
    @Mock
    ObjectRecordStatusEntity objectRecordStatusEntityRawDataFailed;
    @Mock
    private ObjectRecordStatusEntity objectRecordStatusEntityManifestFailed;
    @Mock
    private ObjectRecordStatusEntity objectRecordStatusEntityArmDropZone;
    @Captor
    private ArgumentCaptor<ExternalObjectDirectoryEntity> externalObjectDirectoryEntityCaptor;

    @TempDir
    private File tempDirectory;

    @BeforeEach
    void setUp() {

        unstructuredToArmProcessor = new UnstructuredToArmProcessorImpl(
            externalObjectDirectoryRepository,
            objectRecordStatusRepository,
            externalLocationTypeRepository,
            dataManagementApi,
            armDataManagementApi,
            userAccountRepository,
            armDataManagementConfiguration,
            fileOperationService,
            archiveRecordService
        );

    }

    @Test
    void processMovingDataFromUnstructuredStorageToArm() {
        BinaryData binaryData = BinaryData.fromString(TEST_BINARY_DATA);

        String fileLocation = tempDirectory.getAbsolutePath();
        ArchiveRecordFileInfo archiveRecordFileInfo = ArchiveRecordFileInfo.builder()
            .fileGenerationSuccessful(true)
            .archiveRecordFile(new File(fileLocation, "1_1_1.a360"))
            .build();
        when(archiveRecordService.generateArchiveRecord(any(), anyInt())).thenReturn(archiveRecordFileInfo);

        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeUnstructured);
        when(externalLocationTypeRepository.getReferenceById(3)).thenReturn(externalLocationTypeArm);

        when(objectRecordStatusRepository.getReferenceById(2)).thenReturn(objectRecordStatusEntityStored);
        when(objectRecordStatusRepository.getReferenceById(12)).thenReturn(objectRecordStatusEntityArmIngestion);
        when(objectRecordStatusRepository.getReferenceById(14)).thenReturn(objectRecordStatusEntityRawDataFailed);
        when(objectRecordStatusRepository.getReferenceById(15)).thenReturn(objectRecordStatusEntityManifestFailed);
        when(objectRecordStatusRepository.getReferenceById(13)).thenReturn(objectRecordStatusEntityArmDropZone);

        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityUnstructured));
        when(externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(
            objectRecordStatusEntityStored,
            externalLocationTypeUnstructured,
            externalLocationTypeArm
        )).thenReturn(inboundList);

        when(externalObjectDirectoryEntityUnstructured.getExternalLocationType()).thenReturn(externalLocationTypeUnstructured);
        when(externalLocationTypeUnstructured.getId()).thenReturn(ExternalLocationTypeEnum.UNSTRUCTURED.getId());
        when(externalObjectDirectoryEntityUnstructured.getExternalLocationType()).thenReturn(externalLocationTypeUnstructured);
        when(dataManagementApi.getBlobDataFromUnstructuredContainer(any())).thenReturn(binaryData);

        unstructuredToArmProcessor.processUnstructuredToArm();

        verify(externalObjectDirectoryRepository, times(3)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

    }

    @Test
    void processMovingDataFromUnstructuredStorageToArmWhereBlobExists() {
        BinaryData binaryData = BinaryData.fromString(TEST_BINARY_DATA);

        String fileLocation = tempDirectory.getAbsolutePath();
        ArchiveRecordFileInfo archiveRecordFileInfo = ArchiveRecordFileInfo.builder()
            .fileGenerationSuccessful(true)
            .archiveRecordFile(new File(fileLocation, "1_1_1.a360"))
            .build();
        when(archiveRecordService.generateArchiveRecord(any(), anyInt())).thenReturn(archiveRecordFileInfo);

        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeUnstructured);
        when(externalLocationTypeRepository.getReferenceById(3)).thenReturn(externalLocationTypeArm);

        when(objectRecordStatusRepository.getReferenceById(2)).thenReturn(objectRecordStatusEntityStored);
        when(objectRecordStatusRepository.getReferenceById(12)).thenReturn(objectRecordStatusEntityArmIngestion);
        when(objectRecordStatusRepository.getReferenceById(14)).thenReturn(objectRecordStatusEntityRawDataFailed);
        when(objectRecordStatusRepository.getReferenceById(15)).thenReturn(objectRecordStatusEntityManifestFailed);
        when(objectRecordStatusRepository.getReferenceById(13)).thenReturn(objectRecordStatusEntityArmDropZone);

        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityUnstructured));
        when(externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(
            objectRecordStatusEntityStored,
            externalLocationTypeUnstructured,
            externalLocationTypeArm
        )).thenReturn(inboundList);

        when(externalObjectDirectoryEntityUnstructured.getExternalLocationType()).thenReturn(externalLocationTypeUnstructured);
        when(externalLocationTypeUnstructured.getId()).thenReturn(ExternalLocationTypeEnum.UNSTRUCTURED.getId());
        when(externalObjectDirectoryEntityUnstructured.getExternalLocationType()).thenReturn(externalLocationTypeUnstructured);

        when(dataManagementApi.getBlobDataFromUnstructuredContainer(any())).thenReturn(binaryData);

        // Example: Failed to move BLOB metadata for file /opt/app/arm/tempworkspace/5728_1794_1.a360 due to Status code 409,
        // "﻿<?xml version="1.0" encoding="utf-8"?><Error><Code>BlobAlreadyExists</Code><Message>The specified blob already exists
        BlobStorageException blobStorageException = mock(BlobStorageException.class);
        when(blobStorageException.getStatusCode()).thenReturn(409);
        when(blobStorageException.getMessage()).thenReturn("The specified blob already exists");
        when(armDataManagementApi.saveBlobDataToArm(any(), any())).thenThrow(blobStorageException);

        unstructuredToArmProcessor.processUnstructuredToArm();

        verify(externalObjectDirectoryRepository, times(2)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());
    }

    @Test
    void processMovingDataFromUnstructuredStorageToArmWhereBlobCopyFailed() {
        BinaryData binaryData = BinaryData.fromString(TEST_BINARY_DATA);

        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeUnstructured);
        when(externalLocationTypeRepository.getReferenceById(3)).thenReturn(externalLocationTypeArm);

        when(objectRecordStatusRepository.getReferenceById(2)).thenReturn(objectRecordStatusEntityStored);
        when(objectRecordStatusRepository.getReferenceById(12)).thenReturn(objectRecordStatusEntityArmIngestion);
        when(objectRecordStatusRepository.getReferenceById(14)).thenReturn(objectRecordStatusEntityRawDataFailed);
        when(objectRecordStatusRepository.getReferenceById(15)).thenReturn(objectRecordStatusEntityManifestFailed);
        when(objectRecordStatusRepository.getReferenceById(13)).thenReturn(objectRecordStatusEntityArmDropZone);

        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityUnstructured));
        when(externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(
            objectRecordStatusEntityStored,
            externalLocationTypeUnstructured,
            externalLocationTypeArm
        )).thenReturn(inboundList);

        when(externalObjectDirectoryEntityUnstructured.getExternalLocationType()).thenReturn(externalLocationTypeUnstructured);
        when(externalLocationTypeUnstructured.getId()).thenReturn(ExternalLocationTypeEnum.UNSTRUCTURED.getId());
        when(externalObjectDirectoryEntityUnstructured.getExternalLocationType()).thenReturn(externalLocationTypeUnstructured);

        when(dataManagementApi.getBlobDataFromUnstructuredContainer(any())).thenReturn(binaryData);

        BlobStorageException blobStorageException = mock(BlobStorageException.class);
        when(blobStorageException.getStatusCode()).thenReturn(400);
        when(blobStorageException.getMessage()).thenReturn("Copying blob failed");
        when(armDataManagementApi.saveBlobDataToArm(any(), any())).thenThrow(blobStorageException);

        unstructuredToArmProcessor.processUnstructuredToArm();

        verify(externalObjectDirectoryRepository, times(2)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());
    }

    @Test
    void processMovingDataFromUnstructuredStorageToArmThrowsException() {
        BinaryData binaryData = BinaryData.fromString(TEST_BINARY_DATA);

        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeUnstructured);
        when(externalLocationTypeRepository.getReferenceById(3)).thenReturn(externalLocationTypeArm);

        when(objectRecordStatusRepository.getReferenceById(2)).thenReturn(objectRecordStatusEntityStored);
        when(objectRecordStatusRepository.getReferenceById(12)).thenReturn(objectRecordStatusEntityArmIngestion);
        when(objectRecordStatusRepository.getReferenceById(14)).thenReturn(objectRecordStatusEntityRawDataFailed);
        when(objectRecordStatusRepository.getReferenceById(15)).thenReturn(objectRecordStatusEntityManifestFailed);
        when(objectRecordStatusRepository.getReferenceById(13)).thenReturn(objectRecordStatusEntityArmDropZone);

        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityUnstructured));
        when(externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(
            objectRecordStatusEntityStored,
            externalLocationTypeUnstructured,
            externalLocationTypeArm
        )).thenReturn(inboundList);

        when(externalObjectDirectoryEntityUnstructured.getExternalLocationType()).thenReturn(externalLocationTypeUnstructured);
        when(externalLocationTypeUnstructured.getId()).thenReturn(ExternalLocationTypeEnum.UNSTRUCTURED.getId());
        when(externalObjectDirectoryEntityUnstructured.getExternalLocationType()).thenReturn(externalLocationTypeUnstructured);

        when(dataManagementApi.getBlobDataFromUnstructuredContainer(any())).thenReturn(binaryData);

        NullPointerException genericException = new NullPointerException();
        when(armDataManagementApi.saveBlobDataToArm(any(), any())).thenThrow(genericException);

        unstructuredToArmProcessor.processUnstructuredToArm();

        verify(externalObjectDirectoryRepository, times(2)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());
    }

    @Test
    void processMovingDataFromUnstructuredStorageToArmThrowsBlobExceptionWhenSendingManifestFile() {
        BinaryData binaryData = BinaryData.fromString(TEST_BINARY_DATA);

        String fileLocation = tempDirectory.getAbsolutePath();
        ArchiveRecordFileInfo archiveRecordFileInfo = ArchiveRecordFileInfo.builder()
            .fileGenerationSuccessful(true)
            .archiveRecordFile(new File(fileLocation, "1_1_1.a360"))
            .build();
        when(archiveRecordService.generateArchiveRecord(any(), anyInt())).thenReturn(archiveRecordFileInfo);

        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeUnstructured);
        when(externalLocationTypeRepository.getReferenceById(3)).thenReturn(externalLocationTypeArm);

        when(objectRecordStatusRepository.getReferenceById(2)).thenReturn(objectRecordStatusEntityStored);
        when(objectRecordStatusRepository.getReferenceById(12)).thenReturn(objectRecordStatusEntityArmIngestion);
        when(objectRecordStatusRepository.getReferenceById(14)).thenReturn(objectRecordStatusEntityRawDataFailed);
        when(objectRecordStatusRepository.getReferenceById(15)).thenReturn(objectRecordStatusEntityManifestFailed);
        when(objectRecordStatusRepository.getReferenceById(13)).thenReturn(objectRecordStatusEntityArmDropZone);

        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityUnstructured));
        when(externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(
            objectRecordStatusEntityStored,
            externalLocationTypeUnstructured,
            externalLocationTypeArm
        )).thenReturn(inboundList);

        when(externalObjectDirectoryEntityUnstructured.getExternalLocationType()).thenReturn(externalLocationTypeUnstructured);
        when(externalLocationTypeUnstructured.getId()).thenReturn(ExternalLocationTypeEnum.UNSTRUCTURED.getId());
        when(externalObjectDirectoryEntityUnstructured.getExternalLocationType()).thenReturn(externalLocationTypeUnstructured);

        when(dataManagementApi.getBlobDataFromUnstructuredContainer(any())).thenReturn(binaryData);

        BlobStorageException blobStorageException = mock(BlobStorageException.class);
        when(armDataManagementApi.saveBlobDataToArm(any(), any())).thenReturn("1_1_1").thenThrow(blobStorageException);

        unstructuredToArmProcessor.processUnstructuredToArm();

        verify(externalObjectDirectoryRepository, times(3)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());
    }

    @Test
    void processMovingDataFromUnstructuredStorageToArmThrowsGenericExceptionWhenSendingManifestFile() {
        BinaryData binaryData = BinaryData.fromString(TEST_BINARY_DATA);

        String fileLocation = tempDirectory.getAbsolutePath();
        ArchiveRecordFileInfo archiveRecordFileInfo = ArchiveRecordFileInfo.builder()
            .fileGenerationSuccessful(true)
            .archiveRecordFile(new File(fileLocation, "1_1_1.a360"))
            .build();
        when(archiveRecordService.generateArchiveRecord(any(), anyInt())).thenReturn(archiveRecordFileInfo);

        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeUnstructured);
        when(externalLocationTypeRepository.getReferenceById(3)).thenReturn(externalLocationTypeArm);

        when(objectRecordStatusRepository.getReferenceById(2)).thenReturn(objectRecordStatusEntityStored);
        when(objectRecordStatusRepository.getReferenceById(12)).thenReturn(objectRecordStatusEntityArmIngestion);
        when(objectRecordStatusRepository.getReferenceById(14)).thenReturn(objectRecordStatusEntityRawDataFailed);
        when(objectRecordStatusRepository.getReferenceById(15)).thenReturn(objectRecordStatusEntityManifestFailed);
        when(objectRecordStatusRepository.getReferenceById(13)).thenReturn(objectRecordStatusEntityArmDropZone);

        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityUnstructured));
        when(externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(
            objectRecordStatusEntityStored,
            externalLocationTypeUnstructured,
            externalLocationTypeArm
        )).thenReturn(inboundList);

        when(externalObjectDirectoryEntityUnstructured.getExternalLocationType()).thenReturn(externalLocationTypeUnstructured);
        when(externalLocationTypeUnstructured.getId()).thenReturn(ExternalLocationTypeEnum.UNSTRUCTURED.getId());
        when(externalObjectDirectoryEntityUnstructured.getExternalLocationType()).thenReturn(externalLocationTypeUnstructured);

        when(dataManagementApi.getBlobDataFromUnstructuredContainer(any())).thenReturn(binaryData);

        NullPointerException genericException = new NullPointerException();
        when(armDataManagementApi.saveBlobDataToArm(any(), any())).thenReturn("1_1_1").thenThrow(genericException);

        unstructuredToArmProcessor.processUnstructuredToArm();

        verify(externalObjectDirectoryRepository, times(3)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());
    }

    @Test
    void processPreviousFailedAttemptMovingFromUnstructuredStorageToArm() {

        String fileLocation = tempDirectory.getAbsolutePath();
        ArchiveRecordFileInfo archiveRecordFileInfo = ArchiveRecordFileInfo.builder()
            .fileGenerationSuccessful(true)
            .archiveRecordFile(new File(fileLocation, "1_1_1.a360"))
            .build();
        when(archiveRecordService.generateArchiveRecord(any(), anyInt())).thenReturn(archiveRecordFileInfo);

        when(objectRecordStatusRepository.getReferenceById(2)).thenReturn(objectRecordStatusEntityStored);
        when(objectRecordStatusRepository.getReferenceById(12)).thenReturn(objectRecordStatusEntityArmIngestion);
        when(objectRecordStatusRepository.getReferenceById(14)).thenReturn(objectRecordStatusEntityRawDataFailed);
        when(objectRecordStatusRepository.getReferenceById(15)).thenReturn(objectRecordStatusEntityManifestFailed);
        when(objectRecordStatusRepository.getReferenceById(13)).thenReturn(objectRecordStatusEntityArmDropZone);
        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeUnstructured);
        when(externalLocationTypeRepository.getReferenceById(3)).thenReturn(externalLocationTypeArm);

        List<ExternalObjectDirectoryEntity> pendingUnstructuredStorageItems = new ArrayList<>(Collections.emptyList());
        when(externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(
            objectRecordStatusEntityStored,
            externalLocationTypeUnstructured,
            externalLocationTypeArm
        )).thenReturn(pendingUnstructuredStorageItems);

        List<ObjectRecordStatusEntity> failedStatusesList = new ArrayList<>();
        failedStatusesList.add(objectRecordStatusEntityRawDataFailed);
        failedStatusesList.add(objectRecordStatusEntityManifestFailed);

        when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(MAX_RETRY_ATTEMPTS);
        List<ExternalObjectDirectoryEntity> pendingFailureList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityArm));
        when(externalObjectDirectoryRepository.findNotFinishedAndNotExceededRetryInStorageLocation(
            failedStatusesList,
            externalLocationTypeRepository.getReferenceById(3),
            armDataManagementConfiguration.getMaxRetryAttempts()
        )).thenReturn(pendingFailureList);

        BinaryData binaryData = BinaryData.fromString(TEST_BINARY_DATA);
        when(dataManagementApi.getBlobDataFromUnstructuredContainer(any())).thenReturn(binaryData);
        when(externalObjectDirectoryEntityArm.getExternalLocationType()).thenReturn(externalLocationTypeArm);
        when(externalObjectDirectoryEntityArm.getMedia()).thenReturn(mediaEntity);
        when(externalObjectDirectoryEntityArm.getTranscriptionDocumentEntity()).thenReturn(transcriptionDocumentEntity);
        when(externalObjectDirectoryEntityArm.getAnnotationDocumentEntity()).thenReturn(annotationDocumentEntity);
        when(externalObjectDirectoryRepository.findMatchingExternalObjectDirectoryEntityByLocation(
            objectRecordStatusEntityStored,
            externalLocationTypeUnstructured,
            externalObjectDirectoryEntityArm.getMedia(),
            externalObjectDirectoryEntityArm.getTranscriptionDocumentEntity(),
            externalObjectDirectoryEntityArm.getAnnotationDocumentEntity()
        )).thenReturn(Optional.ofNullable(externalObjectDirectoryEntityUnstructured));

        unstructuredToArmProcessor.processUnstructuredToArm();

        verify(externalObjectDirectoryRepository, times(3)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

    }

    @Test
    void processPreviousFailedAttempt() {

        when(objectRecordStatusRepository.getReferenceById(2)).thenReturn(objectRecordStatusEntityStored);
        when(objectRecordStatusRepository.getReferenceById(12)).thenReturn(objectRecordStatusEntityArmIngestion);
        when(objectRecordStatusRepository.getReferenceById(14)).thenReturn(objectRecordStatusEntityRawDataFailed);
        when(objectRecordStatusRepository.getReferenceById(15)).thenReturn(objectRecordStatusEntityManifestFailed);
        when(objectRecordStatusRepository.getReferenceById(13)).thenReturn(objectRecordStatusEntityArmDropZone);
        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeUnstructured);
        when(externalLocationTypeRepository.getReferenceById(3)).thenReturn(externalLocationTypeArm);

        List<ExternalObjectDirectoryEntity> pendingUnstructuredStorageItems = new ArrayList<>(Collections.emptyList());

        when(externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(
            objectRecordStatusEntityStored,
            externalLocationTypeUnstructured,
            externalLocationTypeArm
        )).thenReturn(pendingUnstructuredStorageItems);


        when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(MAX_RETRY_ATTEMPTS);

        List<ObjectRecordStatusEntity> failedStatusesList = new ArrayList<>();
        failedStatusesList.add(objectRecordStatusEntityRawDataFailed);
        failedStatusesList.add(objectRecordStatusEntityManifestFailed);

        List<ExternalObjectDirectoryEntity> pendingFailureList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityArm));
        when(externalObjectDirectoryRepository.findNotFinishedAndNotExceededRetryInStorageLocation(
            failedStatusesList,
            externalLocationTypeRepository.getReferenceById(3),
            armDataManagementConfiguration.getMaxRetryAttempts()
        )).thenReturn(pendingFailureList);

        when(externalObjectDirectoryEntityArm.getExternalLocationType()).thenReturn(externalLocationTypeArm);
        when(externalObjectDirectoryEntityArm.getMedia()).thenReturn(mediaEntity);
        when(externalObjectDirectoryEntityArm.getTranscriptionDocumentEntity()).thenReturn(transcriptionDocumentEntity);
        when(externalObjectDirectoryEntityArm.getAnnotationDocumentEntity()).thenReturn(annotationDocumentEntity);
        when(externalObjectDirectoryRepository.findMatchingExternalObjectDirectoryEntityByLocation(
            objectRecordStatusEntityStored,
            externalLocationTypeUnstructured,
            externalObjectDirectoryEntityArm.getMedia(),
            externalObjectDirectoryEntityArm.getTranscriptionDocumentEntity(),
            externalObjectDirectoryEntityArm.getAnnotationDocumentEntity()
        )).thenReturn(Optional.empty());

        unstructuredToArmProcessor.processUnstructuredToArm();

        verify(externalObjectDirectoryRepository, times(1)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());
    }
}
