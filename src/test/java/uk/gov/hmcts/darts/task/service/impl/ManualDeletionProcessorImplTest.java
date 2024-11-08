package uk.gov.hmcts.darts.task.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.darts.audio.deleter.impl.inbound.ExternalInboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.unstructured.ExternalUnstructuredDataStoreDeleter;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.test.common.TestUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManualDeletionProcessorImplTest {

    public static final int MEDIA_ID = 100;
    public static final int TRANSCRIPTION_ID = 200;

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
    @Mock
    private LogApi logApi;
    @Mock
    private UserIdentity userIdentity;

    private ManualDeletionProcessorImpl manualDeletionProcessor;

    @BeforeEach
    void setUp() {
        manualDeletionProcessor = new ManualDeletionProcessorImpl(userIdentity,
                                                                  objectAdminActionRepository,
                                                                  externalObjectDirectoryRepository,
                                                                  mediaRepository,
                                                                  transcriptionDocumentRepository,
                                                                  inboundDeleter,
                                                                  unstructuredDeleter,
                                                                  logApi);
        ReflectionTestUtils.setField(manualDeletionProcessor, "gracePeriod", "24h");
    }

    @Test
    void processShouldDeleteMediaAndTranscription() {
        ObjectAdminActionEntity mediaAction = createObjectAdminAction(true, false);
        ObjectAdminActionEntity transcriptionAction = createObjectAdminAction(false, true);
        List<ObjectAdminActionEntity> actionsToDelete = Arrays.asList(mediaAction, transcriptionAction);

        when(objectAdminActionRepository.findFilesForManualDeletion(any(), eq(Limit.of(123)))).thenReturn(actionsToDelete);
        when(externalObjectDirectoryRepository.findStoredInInboundAndUnstructuredByMediaId(any())).thenReturn(
            Collections.singletonList(createExternalObjectDirectoryEntity(ExternalLocationTypeEnum.INBOUND)));
        when(externalObjectDirectoryRepository.findStoredInInboundAndUnstructuredByTranscriptionId(any())).thenReturn(
            Collections.singletonList(createExternalObjectDirectoryEntity(ExternalLocationTypeEnum.UNSTRUCTURED)));

        UserAccountEntity userAccount = mock(UserAccountEntity.class);
        when(userIdentity.getUserAccount()).thenReturn(userAccount);
        manualDeletionProcessor.process(123);

        verify(mediaRepository).save(any(MediaEntity.class));
        verify(transcriptionDocumentRepository).save(any(TranscriptionDocumentEntity.class));
        verify(externalObjectDirectoryRepository, times(2)).delete(any(ExternalObjectDirectoryEntity.class));
        verify(inboundDeleter).delete(any(ExternalObjectDirectoryEntity.class));
        verify(unstructuredDeleter).delete(any(ExternalObjectDirectoryEntity.class));
        verify(logApi, times(1)).mediaDeleted(MEDIA_ID);
        verify(logApi, times(1)).transcriptionDeleted(TRANSCRIPTION_ID);
        verify(userIdentity, times(1)).getUserAccount();

        MediaEntity media = mediaAction.getMedia();
        assertThat(media.isDeleted()).isTrue();
        assertThat(media.getDeletedBy()).isEqualTo(userAccount);
        assertThat(media.getDeletedTs()).isCloseTo(OffsetDateTime.now(), TestUtils.TIME_TOLERANCE);

        TranscriptionDocumentEntity transcriptionDocument = transcriptionAction.getTranscriptionDocument();
        assertThat(transcriptionDocument.isDeleted()).isTrue();
        assertThat(transcriptionDocument.getDeletedBy()).isEqualTo(userAccount);
        assertThat(transcriptionDocument.getDeletedTs()).isCloseTo(OffsetDateTime.now(), TestUtils.TIME_TOLERANCE);
        verify(mediaAction.getMedia(), times(1)).markAsDeleted(userAccount);
        verify(transcriptionAction.getTranscriptionDocument(), times(1)).markAsDeleted(userAccount);
    }

    @Test
    void processShouldNotDeleteAlreadyDeletedEntities() {
        ObjectAdminActionEntity deletedMediaAction = createObjectAdminAction(true, false);
        deletedMediaAction.getMedia().setDeleted(true);
        ObjectAdminActionEntity deletedTranscriptionAction = createObjectAdminAction(false, true);
        deletedTranscriptionAction.getTranscriptionDocument().setDeleted(true);
        List<ObjectAdminActionEntity> actionsToDelete = Arrays.asList(deletedMediaAction, deletedTranscriptionAction);

        when(objectAdminActionRepository.findFilesForManualDeletion(any(), eq(Limit.of(123)))).thenReturn(actionsToDelete);

        manualDeletionProcessor.process(123);

        verify(mediaRepository, never()).save(any(MediaEntity.class));
        verify(transcriptionDocumentRepository, never()).save(any(TranscriptionDocumentEntity.class));
        verify(externalObjectDirectoryRepository, never()).delete(any(ExternalObjectDirectoryEntity.class));
        verify(userIdentity, times(1)).getUserAccount();
    }

    @Test
    void deleteFromExternalDataStoreShouldHandleUnknownLocationType() {
        ExternalObjectDirectoryEntity entity = createExternalObjectDirectoryEntity(null);
        entity.setExternalLocationType(new ExternalLocationTypeEntity());
        entity.getExternalLocationType().setId(99); // Unknown ID

        manualDeletionProcessor.process(123);

        verify(inboundDeleter, never()).delete(any(ExternalObjectDirectoryEntity.class));
        verify(unstructuredDeleter, never()).delete(any(ExternalObjectDirectoryEntity.class));
    }

    @Test
    void getDeletionThresholdShouldReturnCorrectThreshold() {
        LocalDateTime now = LocalDateTime.now();

        OffsetDateTime threshold = manualDeletionProcessor.getDeletionThreshold();

        assertEquals(now.minusHours(24).getDayOfYear(), threshold.getDayOfYear());
        assertEquals(now.minusHours(24).getHour(), threshold.getHour());
    }

    private ObjectAdminActionEntity createObjectAdminAction(boolean isMedia, boolean isTranscription) {
        ObjectAdminActionEntity action = new ObjectAdminActionEntity();
        if (isMedia) {
            MediaEntity media = spy(new MediaEntity());
            media.setId(MEDIA_ID);
            action.setMedia(media);
        }
        if (isTranscription) {
            TranscriptionDocumentEntity transcriptionDocument = spy(new TranscriptionDocumentEntity());
            transcriptionDocument.setId(TRANSCRIPTION_ID);
            action.setTranscriptionDocument(transcriptionDocument);
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