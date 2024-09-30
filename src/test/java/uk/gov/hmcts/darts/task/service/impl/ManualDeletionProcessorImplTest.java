package uk.gov.hmcts.darts.task.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.darts.audio.deleter.impl.inbound.ExternalInboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.unstructured.ExternalUnstructuredDataStoreDeleter;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManualDeletionProcessorImplTest {

    @Mock
    private ObjectAdminActionRepository objectAdminActionRepository;
    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private TranscriptionDocumentRepository transcriptionDocumentRepository;
    @Mock
    private ExternalInboundDataStoreDeleter inboundDeleter;
    @Mock
    private ExternalUnstructuredDataStoreDeleter unstructuredDeleter;

    private ManualDeletionProcessorImpl manualDeletionProcessor;

    @BeforeEach
    void setUp() {
        manualDeletionProcessor = new ManualDeletionProcessorImpl(objectAdminActionRepository,
                                                                  externalObjectDirectoryRepository,
                                                                  mediaRepository,
                                                                  transcriptionDocumentRepository,
                                                                  inboundDeleter,
                                                                  unstructuredDeleter);
        ReflectionTestUtils.setField(manualDeletionProcessor, "gracePeriod", "24h");
    }

    @Test
    void processShouldDeleteMediaAndTranscription() {
        ObjectAdminActionEntity mediaAction = createObjectAdminAction(true, false);
        ObjectAdminActionEntity transcriptionAction = createObjectAdminAction(false, true);
        List<ObjectAdminActionEntity> actionsToDelete = Arrays.asList(mediaAction, transcriptionAction);

        when(objectAdminActionRepository.findFilesForManualDeletion(any())).thenReturn(actionsToDelete);
        when(externalObjectDirectoryRepository.findByMediaStatusAndLocation(any())).thenReturn(
            Collections.singletonList(createExternalObjectDirectoryEntity(ExternalLocationTypeEnum.INBOUND)));
        when(externalObjectDirectoryRepository.findByTranscriptionStatusAndLocation(any())).thenReturn(
            Collections.singletonList(createExternalObjectDirectoryEntity(ExternalLocationTypeEnum.UNSTRUCTURED)));

        manualDeletionProcessor.process();

        verify(mediaRepository).save(any(MediaEntity.class));
        verify(transcriptionDocumentRepository).save(any(TranscriptionDocumentEntity.class));
        verify(externalObjectDirectoryRepository, times(2)).delete(any(ExternalObjectDirectoryEntity.class));
        verify(inboundDeleter).delete(any(ExternalObjectDirectoryEntity.class));
        verify(unstructuredDeleter).delete(any(ExternalObjectDirectoryEntity.class));
    }

    @Test
    void processShouldNotDeleteAlreadyDeletedEntities() {
        ObjectAdminActionEntity deletedMediaAction = createObjectAdminAction(true, false);
        deletedMediaAction.getMedia().setDeleted(true);
        ObjectAdminActionEntity deletedTranscriptionAction = createObjectAdminAction(false, true);
        deletedTranscriptionAction.getTranscriptionDocument().setDeleted(true);
        List<ObjectAdminActionEntity> actionsToDelete = Arrays.asList(deletedMediaAction, deletedTranscriptionAction);

        when(objectAdminActionRepository.findFilesForManualDeletion(any())).thenReturn(actionsToDelete);

        manualDeletionProcessor.process();

        verify(mediaRepository, never()).save(any(MediaEntity.class));
        verify(transcriptionDocumentRepository, never()).save(any(TranscriptionDocumentEntity.class));
        verify(externalObjectDirectoryRepository, never()).delete(any(ExternalObjectDirectoryEntity.class));
    }

    @Test
    void deleteFromExternalDataStoreShouldHandleUnknownLocationType() {
        ExternalObjectDirectoryEntity entity = createExternalObjectDirectoryEntity(null);
        entity.setExternalLocationType(new ExternalLocationTypeEntity());
        entity.getExternalLocationType().setId(99); // Unknown ID

        manualDeletionProcessor.process();

        verify(inboundDeleter, never()).delete(any());
        verify(unstructuredDeleter, never()).delete(any());
    }

    @Test
    void getDeletionThresholdShouldReturnCorrectThreshold() {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime threshold = manualDeletionProcessor.getDeletionThreshold();

        assertEquals(now.minusHours(24).getDayOfYear(), threshold.getDayOfYear());
        assertEquals(now.minusHours(24).getHour(), threshold.getHour());
    }

    private ObjectAdminActionEntity createObjectAdminAction(boolean isMedia, boolean isTranscription) {
        ObjectAdminActionEntity action = new ObjectAdminActionEntity();
        if (isMedia) {
            action.setMedia(new MediaEntity());
        }
        if (isTranscription) {
            action.setTranscriptionDocument(new TranscriptionDocumentEntity());
        }
        return action;
    }

    private ExternalObjectDirectoryEntity createExternalObjectDirectoryEntity(ExternalLocationTypeEnum locationType) {
        ExternalObjectDirectoryEntity entity = new ExternalObjectDirectoryEntity();
        ExternalLocationTypeEntity locationTypeEntity = new ExternalLocationTypeEntity();
        locationTypeEntity.setId(locationType != null ? locationType.getId() : null);
        entity.setExternalLocationType(locationTypeEntity);
        return entity;
    }
}