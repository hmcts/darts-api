package uk.gov.hmcts.darts.arm.service;

import com.azure.core.util.BinaryData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectDirectoryStatusRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.FAILURE_ARM_INGESTION_FAILED;

@ExtendWith(MockitoExtension.class)
class UnstructuredToArmProcessorImplTest {

    public static final String TEST_BINARY_DATA = "test binary data";
    private static final Integer EXAMPLE_ARM_ENTITY_ID = 100;

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ObjectDirectoryStatusRepository objectDirectoryStatusRepository;
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
    ObjectRecordStatusEntity objectRecordStatusEntityFailed;
    @Captor
    private ArgumentCaptor<ExternalObjectDirectoryEntity> externalObjectDirectoryEntityCaptor;

    @BeforeEach
    void setUp() {

        unstructuredToArmProcessor = new UnstructuredToArmProcessorImpl(externalObjectDirectoryRepository,
                                                                        dataManagementApi, armDataManagementApi, userAccountRepository);
                                                                        armDataManagementConfiguration);
    }

    @Test
    void processMovingDataFromUnstructuredStorageToArm() {
        BinaryData binaryData = BinaryData.fromString(TEST_BINARY_DATA);

        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeUnstructured);
        when(externalLocationTypeRepository.getReferenceById(3)).thenReturn(externalLocationTypeArm);
        when(objectRecordStatusRepository.getReferenceById(2)).thenReturn(objectRecordStatusEntityStored);
        when(objectRecordStatusRepository.getReferenceById(12)).thenReturn(objectRecordStatusEntityArmIngestion);
        when(objectRecordStatusRepository.getReferenceById(8)).thenReturn(objectRecordStatusEntityFailed);

        List<ObjectRecordStatusEntity> armStatuses = getArmStatuses();

        ArchiveRecordFileInfo archiveRecordFileInfo = ArchiveRecordFileInfo.builder().build();
        when(archiveRecordService.generateArchiveRecord(any(), anyInt())).thenReturn(archiveRecordFileInfo);

        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityUnstructured));
        when(externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(objectRecordStatusEntityStored,
                                                                                         armStatuses,
                                                                                         externalLocationTypeUnstructured,
                                                                                         externalLocationTypeArm)).thenReturn(inboundList);

        when(externalObjectDirectoryEntityUnstructured.getExternalLocationType()).thenReturn(externalLocationTypeUnstructured);
        when(externalLocationTypeUnstructured.getId()).thenReturn(ExternalLocationTypeEnum.UNSTRUCTURED.getId());
        when(externalObjectDirectoryEntityUnstructured.getExternalLocationType()).thenReturn(externalLocationTypeUnstructured);
        when(dataManagementApi.getBlobDataFromUnstructuredContainer(any())).thenReturn(binaryData);

        unstructuredToArmProcessor.processUnstructuredToArm();

        verify(externalObjectDirectoryRepository, times(2)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

    }

    private List<ObjectRecordStatusEntity> getArmStatuses() {
        List<ObjectRecordStatusEntity> armStatuses = new ArrayList<>();
        armStatuses.add(objectRecordStatusEntityStored);
        armStatuses.add(objectRecordStatusEntityFailed);
        armStatuses.add(objectRecordStatusEntityArmIngestion);

        return armStatuses;
    }

    @Test
    void processPreviousFailedAttemptMovingFromUnstructuredStorageToArm() {
        BinaryData binaryData = BinaryData.fromString(TEST_BINARY_DATA);

        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeUnstructured);
        when(externalLocationTypeRepository.getReferenceById(3)).thenReturn(externalLocationTypeArm);

        when(externalObjectDirectoryEntityArm.getTranscriptionDocumentEntity().getId()).thenReturn(EXAMPLE_TRANSCRIPTION_ID);
        when(objectDirectoryStatusRepository.getReferenceById(2)).thenReturn(objectDirectoryStatusEntityStored);
        when(objectRecordStatusRepository.getReferenceById(12)).thenReturn(objectRecordStatusEntityArmIngestion);
        when(objectRecordStatusRepository.getReferenceById(8)).thenReturn(objectRecordStatusEntityFailed);
        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeUnstructured);
        when(externalLocationTypeRepository.getReferenceById(3)).thenReturn(externalLocationTypeArm);

        List<ObjectRecordStatusEntity> armStatuses = getArmStatuses();

        ArchiveRecordFileInfo archiveRecordFileInfo = ArchiveRecordFileInfo.builder().build();
        when(archiveRecordService.generateArchiveRecord(any(), anyInt())).thenReturn(archiveRecordFileInfo);

        List<ExternalObjectDirectoryEntity> pendingUnstructuredStorageItems = new ArrayList<>(Collections.emptyList());
        when(externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(objectDirectoryStatusEntityStored,
                                                                                         externalLocationTypeUnstructured,
                                                                                         externalLocationTypeArm)).thenReturn(pendingUnstructuredStorageItems);


        when(objectDirectoryStatusRepository.getReferenceById(FAILURE_ARM_INGESTION_FAILED.getId())).thenReturn(objectDirectoryStatusEntityFailed);
        when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(MAX_RETRY_ATTEMPTS);
        List<ExternalObjectDirectoryEntity> pendingFailureList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityArm));
        when(externalObjectDirectoryRepository.findFailedNotExceedRetryInStorageLocation(objectDirectoryStatusEntityFailed,
                                                                                         externalLocationTypeRepository.getReferenceById(3),
                                                                                         armDataManagementConfiguration.getMaxRetryAttempts()))
            .thenReturn(pendingFailureList);

        when(dataManagementApi.getBlobDataFromUnstructuredContainer(any())).thenReturn(binaryData);
        when(externalObjectDirectoryEntityArm.getExternalLocationType()).thenReturn(externalLocationTypeArm);
        when(externalObjectDirectoryEntityArm.getExternalLocationType()).thenReturn(externalLocationTypeArm);
        when(externalObjectDirectoryEntityArm.getMedia()).thenReturn(mediaEntity);
        when(externalObjectDirectoryEntityArm.getTranscriptionDocumentEntity()).thenReturn(transcriptionDocumentEntity);
        when(externalObjectDirectoryEntityArm.getAnnotationDocumentEntity()).thenReturn(annotationDocumentEntity);
        when(externalObjectDirectoryRepository
                 .findMatchingExternalObjectDirectoryEntityByLocation(objectDirectoryStatusEntityStored,
                                                                      externalLocationTypeUnstructured,
                                                                      externalObjectDirectoryEntityArm.getMedia(),
                                                                      externalObjectDirectoryEntityArm.getTranscriptionDocumentEntity(),
                                                                      externalObjectDirectoryEntityArm.getAnnotationDocumentEntity()))
            .thenReturn(Optional.ofNullable(externalObjectDirectoryEntityUnstructured));

        unstructuredToArmProcessor.processUnstructuredToArm();

        verify(externalObjectDirectoryRepository, times(2)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

    }

    @Test
    void processPreviousFailedAttempt() {
        
        when(objectDirectoryStatusRepository.getReferenceById(2)).thenReturn(objectDirectoryStatusEntityStored);
        when(objectRecordStatusRepository.getReferenceById(12)).thenReturn(objectRecordStatusEntityArmIngestion);
        when(objectDirectoryStatusRepository.getReferenceById(8)).thenReturn(objectDirectoryStatusEntityFailed);
        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeUnstructured);
        when(externalLocationTypeRepository.getReferenceById(3)).thenReturn(externalLocationTypeArm);

        List<ExternalObjectDirectoryEntity> pendingUnstructuredStorageItems = new ArrayList<>(Collections.emptyList());

        List<ObjectRecordStatusEntity> armStatuses = getArmStatuses();

        when(externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(objectRecordStatusEntityStored,
                                                                                         armStatuses,
                                                                                         externalLocationTypeUnstructured,
                                                                                         externalLocationTypeArm)).thenReturn(pendingUnstructuredStorageItems);


        when(objectRecordStatusRepository.getReferenceById(FAILURE_ARM_INGESTION_FAILED.getId())).thenReturn(objectRecordStatusEntityFailed);
        when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(MAX_RETRY_ATTEMPTS);
        List<ExternalObjectDirectoryEntity> pendingFailureList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityArm));
        when(externalObjectDirectoryRepository.findFailedNotExceedRetryInStorageLocation(objectRecordStatusEntityFailed,
                                                                                         externalLocationTypeRepository.getReferenceById(3),
                                                                                         armDataManagementConfiguration.getMaxRetryAttempts()))
            .thenReturn(pendingFailureList);

        when(externalObjectDirectoryEntityArm.getExternalLocationType()).thenReturn(externalLocationTypeArm);
        when(externalObjectDirectoryEntityArm.getMedia()).thenReturn(mediaEntity);
        when(externalObjectDirectoryEntityArm.getTranscriptionDocumentEntity()).thenReturn(transcriptionDocumentEntity);
        when(externalObjectDirectoryEntityArm.getAnnotationDocumentEntity()).thenReturn(annotationDocumentEntity);
        when(externalObjectDirectoryRepository
                 .findMatchingExternalObjectDirectoryEntityByLocation(objectRecordStatusEntityStored,
                                                                      externalLocationTypeUnstructured,
                                                                      externalObjectDirectoryEntityArm.getMedia(),
                                                                      externalObjectDirectoryEntityArm.getTranscriptionDocumentEntity(),
                                                                      externalObjectDirectoryEntityArm.getAnnotationDocumentEntity()))
            .thenReturn(Optional.empty());

        unstructuredToArmProcessor.processUnstructuredToArm();

        verify(externalObjectDirectoryRepository, times(1)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());
    }
}
