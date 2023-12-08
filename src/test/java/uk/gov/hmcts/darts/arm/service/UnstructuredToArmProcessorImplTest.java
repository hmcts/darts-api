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
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.FAILURE_ARM_INGESTION_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.STORED;

@ExtendWith(MockitoExtension.class)
class UnstructuredToArmProcessorImplTest {

    private static final String TEST_BINARY_DATA = "test binary data";
    private static final Integer MAX_RETRY_ATTEMPTS = 3;

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
    private UnstructuredToArmProcessor unstructuredToArmProcessor;
    @Mock
    ObjectDirectoryStatusEntity objectDirectoryStatusEntityStored;
    @Mock
    ObjectDirectoryStatusEntity objectDirectoryStatusEntityFailed;
    @Captor
    private ArgumentCaptor<ExternalObjectDirectoryEntity> externalObjectDirectoryEntityCaptor;

    @BeforeEach
    void setUp() {

        unstructuredToArmProcessor = new UnstructuredToArmProcessorImpl(externalObjectDirectoryRepository,
                                                                        objectDirectoryStatusRepository, externalLocationTypeRepository,
                                                                        dataManagementApi, armDataManagementApi, userAccountRepository,
                                                                        armDataManagementConfiguration);
    }

    @Test
    void processMovingDataFromUnstructuredStorageToArm() {
        BinaryData binaryData = BinaryData.fromString(TEST_BINARY_DATA);

        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeUnstructured);
        when(externalLocationTypeRepository.getReferenceById(3)).thenReturn(externalLocationTypeArm);
        when(objectDirectoryStatusRepository.getReferenceById(2)).thenReturn(objectDirectoryStatusEntityStored);

        when(objectDirectoryStatusEntityStored.getId()).thenReturn(2);
        List<ObjectDirectoryStatusEntity> armStatuses = getArmStatuses();

        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityUnstructured));
        when(externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(objectDirectoryStatusEntityStored,
                                                                                         armStatuses,
                                                                                         externalLocationTypeUnstructured,
                                                                                         externalLocationTypeArm)).thenReturn(inboundList);

        when(externalObjectDirectoryEntityUnstructured.getExternalLocationType()).thenReturn(externalLocationTypeUnstructured);
        when(externalLocationTypeUnstructured.getId()).thenReturn(ExternalLocationTypeEnum.UNSTRUCTURED.getId());
        when(externalObjectDirectoryEntityUnstructured.getExternalLocationType()).thenReturn(externalLocationTypeUnstructured);
        when(dataManagementApi.getBlobDataFromUnstructuredContainer(any())).thenReturn(binaryData);

        unstructuredToArmProcessor.processUnstructuredToArm();

        verify(externalObjectDirectoryRepository, times(2)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntityActual = externalObjectDirectoryEntityCaptor.getValue();
        ObjectDirectoryStatusEntity savedStatus = externalObjectDirectoryEntityActual.getStatus();

        assertEquals(STORED.getId(), savedStatus.getId());
    }

    private List<ObjectDirectoryStatusEntity> getArmStatuses() {
        List<ObjectDirectoryStatusEntity> armStatuses = new ArrayList<>();
        armStatuses.add(objectDirectoryStatusEntityStored);
        armStatuses.add(objectDirectoryStatusEntityFailed);
        return armStatuses;
    }

    @Test
    void processPreviousFailedAttemptMovingFromUnstructuredStorageToArm() {
        BinaryData binaryData = BinaryData.fromString(TEST_BINARY_DATA);

        when(objectDirectoryStatusRepository.getReferenceById(2)).thenReturn(objectDirectoryStatusEntityStored);
        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeUnstructured);
        when(externalLocationTypeRepository.getReferenceById(3)).thenReturn(externalLocationTypeArm);

        List<ObjectDirectoryStatusEntity> armStatuses = getArmStatuses();

        List<ExternalObjectDirectoryEntity> pendingUnstructuredStorageItems = new ArrayList<>(Collections.emptyList());
        when(externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(objectDirectoryStatusEntityStored,
                                                                                         armStatuses,
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
        BinaryData binaryData = BinaryData.fromString(TEST_BINARY_DATA);

        when(objectDirectoryStatusRepository.getReferenceById(2)).thenReturn(objectDirectoryStatusEntityStored);
        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeUnstructured);
        when(externalLocationTypeRepository.getReferenceById(3)).thenReturn(externalLocationTypeArm);

        List<ExternalObjectDirectoryEntity> pendingUnstructuredStorageItems = new ArrayList<>(Collections.emptyList());

        List<ObjectDirectoryStatusEntity> armStatuses = getArmStatuses();

        when(externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(objectDirectoryStatusEntityStored,
                                                                                         armStatuses,
                                                                                         externalLocationTypeUnstructured,
                                                                                         externalLocationTypeArm)).thenReturn(pendingUnstructuredStorageItems);


        when(objectDirectoryStatusRepository.getReferenceById(FAILURE_ARM_INGESTION_FAILED.getId())).thenReturn(objectDirectoryStatusEntityFailed);
        when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(MAX_RETRY_ATTEMPTS);
        List<ExternalObjectDirectoryEntity> pendingFailureList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityArm));
        when(externalObjectDirectoryRepository.findFailedNotExceedRetryInStorageLocation(objectDirectoryStatusEntityFailed,
                                                                                         externalLocationTypeRepository.getReferenceById(3),
                                                                                         armDataManagementConfiguration.getMaxRetryAttempts()))
            .thenReturn(pendingFailureList);

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
            .thenReturn(Optional.empty());

        unstructuredToArmProcessor.processUnstructuredToArm();

        verify(externalObjectDirectoryRepository, times(1)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());
    }
}
