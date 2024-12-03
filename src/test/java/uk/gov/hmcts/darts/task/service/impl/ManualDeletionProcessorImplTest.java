package uk.gov.hmcts.darts.task.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.audio.deleter.impl.inbound.ExternalInboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.unstructured.ExternalUnstructuredDataStoreDeleter;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.model.RetConfReason;
import uk.gov.hmcts.darts.test.common.TestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
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
    @Mock
    private ArmDataManagementApi armDataManagementApi;
    @Mock
    private ObjectMapper objectMapper;

    private ManualDeletionProcessorImpl manualDeletionProcessor;

    @BeforeEach
    void setUp() {
        manualDeletionProcessor = spy(new ManualDeletionProcessorImpl(userIdentity,
                                                                      objectAdminActionRepository,
                                                                      externalObjectDirectoryRepository,
                                                                      mediaRepository,
                                                                      transcriptionDocumentRepository,
                                                                      inboundDeleter,
                                                                      unstructuredDeleter,
                                                                      logApi,
                                                                      armDataManagementApi,
                                                                      objectMapper));
        ReflectionTestUtils.setField(manualDeletionProcessor, "gracePeriod", Duration.ofHours(24));
        ReflectionTestUtils.setField(manualDeletionProcessor, "eventDateAdjustmentYears", 100);
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


        doNothing().when(manualDeletionProcessor).processArmEods(any(), any(), anyList());

        ExternalLocationTypeEntity armExternalLocationType = mock(ExternalLocationTypeEntity.class);
        ObjectRecordStatusEntity storedStatus = mock(ObjectRecordStatusEntity.class);
        ReflectionTestUtils.setField(EodHelper.class, "armLocation", armExternalLocationType);
        ReflectionTestUtils.setField(EodHelper.class, "storedStatus", storedStatus);

        List<ExternalObjectDirectoryEntity> mediaEods = List.of(mock(ExternalObjectDirectoryEntity.class));
        List<ExternalObjectDirectoryEntity> transcriptionEods = List.of(mock(ExternalObjectDirectoryEntity.class));
        doReturn(mediaEods).when(externalObjectDirectoryRepository).findByMediaAndExternalLocationTypeAndStatus(any(), any(), any());
        doReturn(transcriptionEods).when(externalObjectDirectoryRepository).findByTranscriptionDocumentEntityAndExternalLocationTypeAndStatus(any(),
                                                                                                                                                    any(),
                                                                                                                                                    any());
        manualDeletionProcessor.process(123);

        verify(mediaRepository).save(any(MediaEntity.class));
        verify(transcriptionDocumentRepository).save(any(TranscriptionDocumentEntity.class));
        verify(externalObjectDirectoryRepository, times(2)).delete(any(ExternalObjectDirectoryEntity.class));
        verify(inboundDeleter).delete(any(ExternalObjectDirectoryEntity.class));
        verify(unstructuredDeleter).delete(any(ExternalObjectDirectoryEntity.class));
        verify(logApi).mediaDeleted(MEDIA_ID);
        verify(logApi).transcriptionDeleted(TRANSCRIPTION_ID);
        verify(userIdentity).getUserAccount();

        MediaEntity media = mediaAction.getMedia();
        assertThat(media.isDeleted()).isTrue();
        assertThat(media.getDeletedBy()).isEqualTo(userAccount);
        assertThat(media.getDeletedTs()).isCloseTo(OffsetDateTime.now(), TestUtils.TIME_TOLERANCE);

        TranscriptionDocumentEntity transcriptionDocument = transcriptionAction.getTranscriptionDocument();
        assertThat(transcriptionDocument.isDeleted()).isTrue();
        assertThat(transcriptionDocument.getDeletedBy()).isEqualTo(userAccount);
        assertThat(transcriptionDocument.getDeletedTs()).isCloseTo(OffsetDateTime.now(), TestUtils.TIME_TOLERANCE);
        verify(mediaAction.getMedia()).markAsDeleted(userAccount);
        verify(transcriptionAction.getTranscriptionDocument()).markAsDeleted(userAccount);

        verify(externalObjectDirectoryRepository).findByTranscriptionDocumentEntityAndExternalLocationTypeAndStatus(transcriptionDocument,
                                                                                                                          armExternalLocationType,
                                                                                                                          storedStatus);
        verify(externalObjectDirectoryRepository).findByMediaAndExternalLocationTypeAndStatus(media, armExternalLocationType, storedStatus);
        verify(manualDeletionProcessor).processArmEods(media.getDeletedTs(), mediaAction, mediaEods);
        verify(manualDeletionProcessor).processArmEods(transcriptionDocument.getDeletedTs(), transcriptionAction, transcriptionEods);
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
        verify(userIdentity).getUserAccount();
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

    @Test
    void processArmEodsTypical() {
        List<ExternalObjectDirectoryEntity> eods = List.of(mock(ExternalObjectDirectoryEntity.class),
                                                           mock(ExternalObjectDirectoryEntity.class),
                                                           mock(ExternalObjectDirectoryEntity.class));
        doNothing().when(manualDeletionProcessor).processArmEod(any(), any(), any());

        OffsetDateTime deletedTs = OffsetDateTime.now();
        ObjectAdminActionEntity objectAdminAction = mock(ObjectAdminActionEntity.class);

        manualDeletionProcessor.processArmEods(deletedTs, objectAdminAction, eods);

        eods.forEach(externalObjectDirectoryEntity ->
                         verify(manualDeletionProcessor).processArmEod(deletedTs, objectAdminAction, externalObjectDirectoryEntity));
    }

    @Test
    void processArmEodTypical() {
        OffsetDateTime deletedTs = OffsetDateTime.now();
        OffsetDateTime expectedEventTs = deletedTs.minusYears(100);
        ObjectAdminActionEntity objectAdminAction = mock(ObjectAdminActionEntity.class);
        ExternalObjectDirectoryEntity eod = mock(ExternalObjectDirectoryEntity.class);
        String externalRecordId = "TestExternalRecordId";
        String retConfReason = "TestRetConReason";
        when(eod.getExternalRecordId()).thenReturn(externalRecordId);
        doReturn(retConfReason).when(manualDeletionProcessor).getRetConfReason(any(), any());

        manualDeletionProcessor.processArmEod(deletedTs, objectAdminAction, eod);

        verify(eod).getExternalRecordId();
        verify(armDataManagementApi).updateMetadata(externalRecordId, expectedEventTs, retConfReason);
        verify(manualDeletionProcessor).getRetConfReason(deletedTs, objectAdminAction);
        verify(externalObjectDirectoryRepository).delete(eod);
    }

    @Test
    void getRetConfReasonTypical() throws Exception {
        final OffsetDateTime deletedTs = OffsetDateTime.now();
        final String reason = "TestReason";
        final String ticketReference = "TestTicketReference";
        final String comments = "TestComments";
        final ObjectAdminActionEntity objectAdminAction = mock(ObjectAdminActionEntity.class);
        final ObjectHiddenReasonEntity objectHiddenReason = mock(ObjectHiddenReasonEntity.class);

        when(objectAdminAction.getObjectHiddenReason()).thenReturn(objectHiddenReason);
        when(objectHiddenReason.getReason()).thenReturn(reason);

        when(objectAdminAction.getTicketReference()).thenReturn(ticketReference);
        when(objectAdminAction.getComments()).thenReturn(comments);
        when(objectAdminAction.getTicketReference()).thenReturn(ticketReference);
        when(objectAdminAction.getComments()).thenReturn(comments);
        when(objectMapper.writeValueAsString(any())).thenReturn("ObjectMapperReturnValue");

        //Perform test
        final String retConfReason = manualDeletionProcessor.getRetConfReason(deletedTs, objectAdminAction);
        assertEquals("ObjectMapperReturnValue", retConfReason);
        //Verify calls
        verify(objectAdminAction).getObjectHiddenReason();
        verify(objectHiddenReason).getReason();

        verify(objectAdminAction).getTicketReference();
        verify(objectAdminAction).getComments();

        //Validate RetConfReason
        ArgumentCaptor<RetConfReason> captor = ArgumentCaptor.forClass(RetConfReason.class);
        verify(objectMapper).writeValueAsString(captor.capture());
        RetConfReason retConfReasonObject = captor.getValue();
        assertEquals(deletedTs, retConfReasonObject.getManualDeletionTs());
        assertEquals(reason, retConfReasonObject.getManualDeletionReason());
        assertEquals(ticketReference, retConfReasonObject.getTicketReference());
        assertEquals(comments, retConfReasonObject.getComments());
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