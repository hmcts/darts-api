package uk.gov.hmcts.darts.common.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.deleter.impl.outbound.ExternalOutboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.DataAnonymisationEntity;
import uk.gov.hmcts.darts.common.entity.DefenceEntity;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.ProsecutorEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.DataAnonymisationRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.event.service.EventService;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.test.common.TestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataAnonymisationServiceImplTest {

    @Mock
    private AuditApi auditApi;
    @Mock
    private CurrentTimeHelper currentTimeHelper;
    @Mock
    private ExternalOutboundDataStoreDeleter outboundDataStoreDeleter;
    @Mock
    private CaseService caseService;
    @Mock
    private TransformedMediaRepository transformedMediaRepository;
    @Mock
    private TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    @Mock
    private LogApi logApi;
    @Mock
    private EventService eventService;
    @Mock
    private DataAnonymisationRepository dataAnonymisationRepository;

    @InjectMocks
    @Spy
    private DataAnonymisationServiceImpl dataAnonymisationService;


    private OffsetDateTime offsetDateTime;

    private void setupOffsetDateTime() {
        offsetDateTime = OffsetDateTime.now();
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(offsetDateTime);
    }

    private void assertLastModifiedByAndAt(CreatedModifiedBaseEntity entity, UserAccountEntity userAccount) {
        assertThat(entity.getLastModifiedById()).isEqualTo(userAccount.getId());
        assertThat(entity.getLastModifiedDateTime()).isEqualTo(offsetDateTime);
    }


    @Test
    @DisplayName("Event should not be anonymised if one or more associated cases are not anonymised")
    void eventEntityAnonymiseNotUpdatedAsNotAllCasesExpiredAndOnlyAnonymiseIfAllCasesExpiredIsTrue() {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventText("event text");

        UserAccountEntity userAccount = new UserAccountEntity();
        when(eventService.allAssociatedCasesAnonymised(eventEntity)).thenReturn(false);
        dataAnonymisationService.anonymiseEventEntity(userAccount, eventEntity, true, false);
        assertThat(eventEntity.getEventText()).isEqualTo("event text");
        assertThat(eventEntity.isDataAnonymised()).isFalse();
        verify(eventService).allAssociatedCasesAnonymised(eventEntity);
        verify(eventService, never()).saveEvent(eventEntity);
    }

    @ParameterizedTest(name = "Event should be anonymised if all associated cases are anonymised. (isManuallyRequested = {0})")
    @ValueSource(booleans = {true, false})
    void anonymiseEventEntity_eventEntityAnonymiseUpdated_asAllCasesExpiredAndOnlyAnonymiseIfAllCasesExpiredIsTrue(boolean isManuallyRequested) {
        setupOffsetDateTime();
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventText("event text");
        eventEntity.setId(123);

        UserAccountEntity userAccount = new UserAccountEntity();
        when(eventService.allAssociatedCasesAnonymised(eventEntity)).thenReturn(true);
        doNothing().when(dataAnonymisationService).registerDataAnonymisation(any(), any(EventEntity.class), anyBoolean());

        dataAnonymisationService.anonymiseEventEntity(userAccount, eventEntity, true, isManuallyRequested);

        assertThat(eventEntity.getEventText()).matches(TestUtils.UUID_REGEX);
        assertThat(eventEntity.isDataAnonymised()).isTrue();
        assertLastModifiedByAndAt(eventEntity, userAccount);
        verify(eventService).allAssociatedCasesAnonymised(eventEntity);
        verify(eventService).saveEvent(eventEntity);
        verify(dataAnonymisationService).registerDataAnonymisation(userAccount, eventEntity, isManuallyRequested);
        if (isManuallyRequested) {
            verify(auditApi).record(AuditActivity.MANUAL_OBFUSCATION, userAccount, "123");
            verify(logApi).manualObfuscation(eventEntity);
        } else {
            verifyNoInteractions(auditApi, logApi);
        }
    }

    @Test
    @DisplayName("Event should be anonymised if one or more associated cases are not anonymised and the onlyAnonymiseIfAllCasesExpired flag is false")
    void anonymiseEventEntity_eventEntityAnonymiseUpdated_asNotAllCasesExpiredAndOnlyAnonymiseIfAllCasesExpiredIsFalse() {
        setupOffsetDateTime();
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventText("event text");

        UserAccountEntity userAccount = new UserAccountEntity();
        dataAnonymisationService.anonymiseEventEntity(userAccount, eventEntity, false, false);
        assertThat(eventEntity.getEventText()).matches(TestUtils.UUID_REGEX);
        assertThat(eventEntity.isDataAnonymised()).isTrue();
        assertLastModifiedByAndAt(eventEntity, userAccount);
        verify(eventService).saveEvent(eventEntity);
    }

    @Test
    @DisplayName("Event should not be anonymised again if the event is already anonymised")
    void anonymiseEventEntity_eventEntityNotUpdatedAsAlreadyAnonymised() {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventText("event text");
        eventEntity.setDataAnonymised(true);

        UserAccountEntity userAccount = new UserAccountEntity();
        dataAnonymisationService.anonymiseEventEntity(userAccount, eventEntity, false, false);
        assertThat(eventEntity.getEventText()).isEqualTo("event text");
        assertThat(eventEntity.isDataAnonymised()).isTrue();
        verifyNoMoreInteractions(eventService);
        verify(eventService, never()).saveEvent(eventEntity);
    }

    @Test
    void positiveAnonymiseDefenceEntity() {
        setupOffsetDateTime();
        DefenceEntity defenceEntity = new DefenceEntity();
        defenceEntity.setName("name");

        UserAccountEntity userAccount = new UserAccountEntity();

        dataAnonymisationService.anonymiseDefenceEntity(userAccount, defenceEntity);
        assertThat(defenceEntity.getName()).matches(TestUtils.UUID_REGEX);
        assertLastModifiedByAndAt(defenceEntity, userAccount);
    }

    @Test
    void positiveAnonymiseDefendantEntity() {
        setupOffsetDateTime();
        DefendantEntity defendantEntity = new DefendantEntity();
        defendantEntity.setName("name");

        UserAccountEntity userAccount = new UserAccountEntity();
        dataAnonymisationService.anonymiseDefendantEntity(userAccount, defendantEntity);
        assertThat(defendantEntity.getName()).matches(TestUtils.UUID_REGEX);
        assertLastModifiedByAndAt(defendantEntity, userAccount);
    }

    @Test
    void positiveAnonymiseProsecutorEntity() {
        setupOffsetDateTime();
        ProsecutorEntity prosecutorEntity = new ProsecutorEntity();
        prosecutorEntity.setName("name");

        UserAccountEntity userAccount = new UserAccountEntity();
        dataAnonymisationService.anonymiseProsecutorEntity(userAccount, prosecutorEntity);

        assertThat(prosecutorEntity.getName()).matches(TestUtils.UUID_REGEX);
        assertLastModifiedByAndAt(prosecutorEntity, userAccount);
    }

    @ParameterizedTest(name = "Anonymise TranscriptionCommentEntity with isManuallyRequested = {0}")
    @ValueSource(booleans = {true, false})
    void anonymiseTranscriptionCommentEntity_typical(boolean isManuallyRequested) {
        setupOffsetDateTime();
        TranscriptionCommentEntity transcriptionCommentEntity = new TranscriptionCommentEntity();
        transcriptionCommentEntity.setComment("comment");

        UserAccountEntity userAccount = new UserAccountEntity();
        doNothing().when(dataAnonymisationService).registerDataAnonymisation(any(), any(TranscriptionCommentEntity.class), anyBoolean());

        dataAnonymisationService.anonymiseTranscriptionCommentEntity(userAccount, transcriptionCommentEntity, isManuallyRequested);

        assertThat(transcriptionCommentEntity.getComment()).matches(TestUtils.UUID_REGEX);
        assertThat(transcriptionCommentEntity.getLastModifiedById()).isEqualTo(userAccount.getId());
        assertThat(transcriptionCommentEntity.isDataAnonymised()).isTrue();
        assertLastModifiedByAndAt(transcriptionCommentEntity, userAccount);
        verify(dataAnonymisationService).registerDataAnonymisation(userAccount, transcriptionCommentEntity, isManuallyRequested);
    }


    @ParameterizedTest(name = "Anonymise CourtCase with isManuallyRequested = {0}")
    @ValueSource(booleans = {true, false})
    void anonymiseCourtCaseById_typical(boolean isManuallyRequested) {
        setupOffsetDateTime();
        CourtCaseEntity courtCase = new CourtCaseEntity();
        when(caseService.getCourtCaseById(123)).thenReturn(courtCase);
        courtCase.setCaseNumber("caseNo123");
        courtCase.setId(123);

        DefendantEntity defendantEntity1 = mock(DefendantEntity.class);
        DefendantEntity defendantEntity2 = mock(DefendantEntity.class);
        courtCase.setDefendantList(List.of(defendantEntity1, defendantEntity2));

        DefenceEntity defenceEntity1 = mock(DefenceEntity.class);
        DefenceEntity defenceEntity2 = mock(DefenceEntity.class);
        courtCase.setDefenceList(List.of(defenceEntity1, defenceEntity2));

        ProsecutorEntity prosecutorEntity1 = mock(ProsecutorEntity.class);
        ProsecutorEntity prosecutorEntity2 = mock(ProsecutorEntity.class);
        courtCase.setProsecutorList(List.of(prosecutorEntity1, prosecutorEntity2));

        HearingEntity hearingEntity1 = mock(HearingEntity.class);
        HearingEntity hearingEntity2 = mock(HearingEntity.class);
        courtCase.setHearings(List.of(hearingEntity1, hearingEntity2));

        EventEntity eventEntity1 = mock(EventEntity.class);
        when(eventEntity1.isDataAnonymised()).thenReturn(false);
        EventEntity eventEntity2 = mock(EventEntity.class);
        when(eventEntity2.isDataAnonymised()).thenReturn(false);

        when(eventService.getAllCourtCaseEventVersions(courtCase)).thenReturn(Set.of(eventEntity1, eventEntity2));
        when(eventService.allAssociatedCasesAnonymised(any())).thenReturn(true);
        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(123);
        doNothing().when(dataAnonymisationService).registerDataAnonymisation(any(), any(EventEntity.class), anyBoolean());

        dataAnonymisationService.anonymiseCourtCaseById(userAccount, 123, isManuallyRequested);
        assertThat(courtCase.isDataAnonymised()).isTrue();
        assertThat(courtCase.getDataAnonymisedBy()).isEqualTo(123);
        assertThat(courtCase.getDataAnonymisedTs()).isCloseToUtcNow(within(5, SECONDS));

        verify(dataAnonymisationService).anonymiseDefendantEntity(userAccount, defendantEntity1);
        verify(dataAnonymisationService).anonymiseDefendantEntity(userAccount, defendantEntity2);

        verify(dataAnonymisationService).anonymiseDefenceEntity(userAccount, defenceEntity1);
        verify(dataAnonymisationService).anonymiseDefenceEntity(userAccount, defenceEntity2);

        verify(dataAnonymisationService).anonymiseProsecutorEntity(userAccount, prosecutorEntity1);
        verify(dataAnonymisationService).anonymiseProsecutorEntity(userAccount, prosecutorEntity2);

        verify(dataAnonymisationService).anonymiseHearingEntity(userAccount, hearingEntity1, isManuallyRequested);
        verify(dataAnonymisationService).anonymiseHearingEntity(userAccount, hearingEntity2, isManuallyRequested);

        verify(dataAnonymisationService).anonymiseEventEntity(userAccount, eventEntity1, true, isManuallyRequested);
        verify(dataAnonymisationService).anonymiseEventEntity(userAccount, eventEntity2, true, isManuallyRequested);

        verify(dataAnonymisationService).tidyUpTransformedMediaEntities(userAccount, courtCase);
        verify(caseService).saveCase(courtCase);
        verify(logApi).caseDeletedDueToExpiry(123, "caseNo123");
        verify(caseService).getCourtCaseById(123);
        verify(dataAnonymisationService).registerDataAnonymisation(userAccount, eventEntity1, isManuallyRequested);
        verify(dataAnonymisationService).registerDataAnonymisation(userAccount, eventEntity2, isManuallyRequested);
        verify(eventService).allAssociatedCasesAnonymised(eventEntity1);
        verify(eventService).allAssociatedCasesAnonymised(eventEntity2);
    }


    @ParameterizedTest(name = "Anonymise Transcription with isManuallyRequested = {0}")
    @ValueSource(booleans = {true, false})
    void anonymiseTranscriptionEntity_typical(boolean isManuallyRequested) {
        TranscriptionEntity transcriptionEntity = new TranscriptionEntity();

        TranscriptionCommentEntity transcriptionCommentEntity1 = mock(TranscriptionCommentEntity.class);
        TranscriptionCommentEntity transcriptionCommentEntity2 = mock(TranscriptionCommentEntity.class);
        transcriptionEntity.setTranscriptionCommentEntities(List.of(transcriptionCommentEntity1, transcriptionCommentEntity2));

        TranscriptionWorkflowEntity transcriptionWorkflowEntity1 = mock(TranscriptionWorkflowEntity.class);
        TranscriptionWorkflowEntity transcriptionWorkflowEntity2 = mock(TranscriptionWorkflowEntity.class);
        transcriptionEntity.setTranscriptionWorkflowEntities(List.of(transcriptionWorkflowEntity1, transcriptionWorkflowEntity2));

        doNothing().when(dataAnonymisationService).anonymiseTranscriptionCommentEntity(any(), any(), anyBoolean());
        doNothing().when(dataAnonymisationService).anonymiseTranscriptionWorkflowEntity(any());


        UserAccountEntity userAccount = new UserAccountEntity();
        dataAnonymisationService.anonymiseTranscriptionEntity(userAccount, transcriptionEntity, isManuallyRequested);

        verify(dataAnonymisationService).anonymiseTranscriptionCommentEntity(userAccount, transcriptionCommentEntity1, isManuallyRequested);
        verify(dataAnonymisationService).anonymiseTranscriptionCommentEntity(userAccount, transcriptionCommentEntity2, isManuallyRequested);

        verify(dataAnonymisationService).anonymiseTranscriptionWorkflowEntity(transcriptionWorkflowEntity1);
        verify(dataAnonymisationService).anonymiseTranscriptionWorkflowEntity(transcriptionWorkflowEntity2);
    }

    @ParameterizedTest(name = "Anonymise hearing with isManuallyRequested = {0}")
    @ValueSource(booleans = {true, false})
    void anonymiseHearingEntity_typical(boolean isManuallyRequested) {
        HearingEntity hearingEntity = new HearingEntity();

        TranscriptionEntity transcriptionEntity1 = mock(TranscriptionEntity.class);
        TranscriptionEntity transcriptionEntity2 = mock(TranscriptionEntity.class);
        hearingEntity.setTranscriptions(List.of(transcriptionEntity1, transcriptionEntity2));

        EventEntity entityEntity1 = mock(EventEntity.class);
        EventEntity entityEntity2 = mock(EventEntity.class);
        hearingEntity.setEvents(Set.of(entityEntity1, entityEntity2));

        doNothing().when(dataAnonymisationService).anonymiseTranscriptionEntity(any(), any(), anyBoolean());

        UserAccountEntity userAccount = new UserAccountEntity();
        dataAnonymisationService.anonymiseHearingEntity(userAccount, hearingEntity, isManuallyRequested);

        verify(dataAnonymisationService).anonymiseTranscriptionEntity(userAccount, transcriptionEntity1, isManuallyRequested);
        verify(dataAnonymisationService).anonymiseTranscriptionEntity(userAccount, transcriptionEntity2, isManuallyRequested);
    }


    @Test
    void positiveExpireMediaRequest() {
        setupOffsetDateTime();
        MediaRequestEntity mediaRequestEntity = new MediaRequestEntity();
        UserAccountEntity userAccount = new UserAccountEntity();

        dataAnonymisationService.expiredMediaRequest(userAccount, mediaRequestEntity);

        assertThat(mediaRequestEntity.getStatus()).isEqualTo(MediaRequestStatus.EXPIRED);
        assertLastModifiedByAndAt(mediaRequestEntity, userAccount);
    }

    @Test
    void positiveDeleteTransientObjectDirectoryEntrySuccess() {
        TransientObjectDirectoryEntity transientObjectDirectoryEntity = new TransientObjectDirectoryEntity();
        when(outboundDataStoreDeleter.delete(any(TransientObjectDirectoryEntity.class))).thenReturn(true);

        dataAnonymisationService.deleteTransientObjectDirectoryEntity(transientObjectDirectoryEntity);

        verify(outboundDataStoreDeleter).delete(transientObjectDirectoryEntity);
        verify(transientObjectDirectoryRepository).delete(transientObjectDirectoryEntity);
    }

    @Test
    void negativeDeleteTransientObjectDirectoryEntryFailure() {
        TransientObjectDirectoryEntity transientObjectDirectoryEntity = new TransientObjectDirectoryEntity();
        when(outboundDataStoreDeleter.delete(any(TransientObjectDirectoryEntity.class))).thenReturn(false);

        dataAnonymisationService.deleteTransientObjectDirectoryEntity(transientObjectDirectoryEntity);

        verify(outboundDataStoreDeleter).delete(transientObjectDirectoryEntity);
        verify(transientObjectDirectoryRepository, never()).delete(transientObjectDirectoryEntity);

    }

    @Test
    void positiveDeleteTransformedMediaEntity() {
        TransformedMediaEntity transformedMediaEntity = new TransformedMediaEntity();
        TransientObjectDirectoryEntity transientObjectDirectoryEntity1 = mock(TransientObjectDirectoryEntity.class);
        TransientObjectDirectoryEntity transientObjectDirectoryEntity2 = mock(TransientObjectDirectoryEntity.class);
        TransientObjectDirectoryEntity transientObjectDirectoryEntity3 = mock(TransientObjectDirectoryEntity.class);
        transformedMediaEntity.setTransientObjectDirectoryEntities(
            List.of(transientObjectDirectoryEntity1, transientObjectDirectoryEntity2, transientObjectDirectoryEntity3));

        doReturn(true).when(dataAnonymisationService).deleteTransientObjectDirectoryEntity(any());

        dataAnonymisationService.deleteTransformedMediaEntity(transformedMediaEntity);

        verify(dataAnonymisationService).deleteTransientObjectDirectoryEntity(transientObjectDirectoryEntity1);
        verify(dataAnonymisationService).deleteTransientObjectDirectoryEntity(transientObjectDirectoryEntity2);
        verify(dataAnonymisationService).deleteTransientObjectDirectoryEntity(transientObjectDirectoryEntity3);
        verify(transformedMediaRepository).delete(transformedMediaEntity);
    }

    @Test
    void positiveTidyUpTransformedMediaEntities() {
        CourtCaseEntity courtCase = new CourtCaseEntity();
        HearingEntity hearingEntity1 = new HearingEntity();
        HearingEntity hearingEntity2 = new HearingEntity();
        HearingEntity hearingEntity3 = new HearingEntity();
        courtCase.setHearings(List.of(hearingEntity1, hearingEntity2, hearingEntity3));

        MediaRequestEntity hearing1MediaRequestEntity1 = new MediaRequestEntity();
        MediaRequestEntity hearing1MediaRequestEntity2 = new MediaRequestEntity();
        MediaRequestEntity hearing1MediaRequestEntity3 = new MediaRequestEntity();
        hearingEntity1.setMediaRequests(List.of(hearing1MediaRequestEntity1, hearing1MediaRequestEntity2, hearing1MediaRequestEntity3));

        MediaRequestEntity hearing2MediaRequestEntity1 = new MediaRequestEntity();
        MediaRequestEntity hearing2MediaRequestEntity2 = new MediaRequestEntity();
        MediaRequestEntity hearing2MediaRequestEntity3 = new MediaRequestEntity();
        hearingEntity2.setMediaRequests(List.of(hearing2MediaRequestEntity1, hearing2MediaRequestEntity2, hearing2MediaRequestEntity3));

        MediaRequestEntity hearing3MediaRequestEntity1 = new MediaRequestEntity();
        hearingEntity3.setMediaRequests(List.of(hearing3MediaRequestEntity1, hearing1MediaRequestEntity1, hearing2MediaRequestEntity2));

        doNothing().when(dataAnonymisationService).expiredMediaRequest(any(), any());
        doNothing().when(dataAnonymisationService).deleteTransformedMediaEntity(any());

        UserAccountEntity userAccount = new UserAccountEntity();


        TransformedMediaEntity transformedMediaEntity1 = new TransformedMediaEntity();
        TransformedMediaEntity transformedMediaEntity2 = new TransformedMediaEntity();
        TransformedMediaEntity transformedMediaEntity3 = new TransformedMediaEntity();
        TransformedMediaEntity transformedMediaEntity4 = new TransformedMediaEntity();
        TransformedMediaEntity transformedMediaEntity5 = new TransformedMediaEntity();

        hearing1MediaRequestEntity1.setTransformedMediaEntities(List.of(
            transformedMediaEntity1, transformedMediaEntity2, transformedMediaEntity3
        ));

        hearing1MediaRequestEntity1.setTransformedMediaEntities(List.of(
            transformedMediaEntity1, transformedMediaEntity2, transformedMediaEntity3
        ));

        hearing1MediaRequestEntity2.setTransformedMediaEntities(List.of(
            transformedMediaEntity1, transformedMediaEntity3
        ));

        hearing2MediaRequestEntity1.setTransformedMediaEntities(List.of(
            transformedMediaEntity4, transformedMediaEntity5
        ));

        hearing3MediaRequestEntity1.setTransformedMediaEntities(List.of(
            transformedMediaEntity4
        ));


        dataAnonymisationService.tidyUpTransformedMediaEntities(userAccount, courtCase);

        verify(dataAnonymisationService).expiredMediaRequest(userAccount, hearing1MediaRequestEntity1);
        verify(dataAnonymisationService).expiredMediaRequest(userAccount, hearing1MediaRequestEntity2);
        verify(dataAnonymisationService).expiredMediaRequest(userAccount, hearing1MediaRequestEntity3);


        verify(dataAnonymisationService).expiredMediaRequest(userAccount, hearing2MediaRequestEntity1);
        verify(dataAnonymisationService).expiredMediaRequest(userAccount, hearing2MediaRequestEntity2);
        verify(dataAnonymisationService).expiredMediaRequest(userAccount, hearing2MediaRequestEntity3);

        verify(dataAnonymisationService).expiredMediaRequest(userAccount, hearing3MediaRequestEntity1);


        verify(dataAnonymisationService)
            .deleteTransformedMediaEntity(transformedMediaEntity1);
        verify(dataAnonymisationService)
            .deleteTransformedMediaEntity(transformedMediaEntity2);
        verify(dataAnonymisationService)
            .deleteTransformedMediaEntity(transformedMediaEntity3);
        verify(dataAnonymisationService)
            .deleteTransformedMediaEntity(transformedMediaEntity4);
        verify(dataAnonymisationService)
            .deleteTransformedMediaEntity(transformedMediaEntity5);
    }

    @ParameterizedTest(name = "Anonymise event by ids with isManuallyRequested = {0}")
    @ValueSource(booleans = {true, false})
    void anonymiseEventByIds_typical(boolean isManuallyRequested) {
        EventEntity event1 = mock(EventEntity.class);
        EventEntity event2 = mock(EventEntity.class);
        EventEntity event3 = mock(EventEntity.class);


        doReturn(event1).when(eventService).getEventByEveId(1);
        doReturn(event2).when(eventService).getEventByEveId(2);
        doReturn(event3).when(eventService).getEventByEveId(3);
        doReturn(event1).when(eventService).getEventByEveId(4);
        UserAccountEntity userAccount = mock(UserAccountEntity.class);
        doNothing().when(dataAnonymisationService).registerDataAnonymisation(any(), any(EventEntity.class), anyBoolean());

        dataAnonymisationService.anonymiseEventByIds(userAccount, List.of(1, 2, 3, 4), isManuallyRequested);


        verify(dataAnonymisationService).anonymiseEventEntity(userAccount, event1, false, isManuallyRequested);
        verify(dataAnonymisationService).anonymiseEventEntity(userAccount, event2, false, isManuallyRequested);
        verify(dataAnonymisationService).anonymiseEventEntity(userAccount, event3, false, isManuallyRequested);

        verify(eventService).getEventByEveId(1);
        verify(eventService).getEventByEveId(2);
        verify(eventService).getEventByEveId(3);
        verify(eventService).getEventByEveId(4);

        verify(eventService).saveEvent(event1);
        verify(eventService).saveEvent(event2);
        verify(eventService).saveEvent(event3);
        verifyNoMoreInteractions(eventService);
        verify(dataAnonymisationService).registerDataAnonymisation(userAccount, event1, isManuallyRequested);
        verify(dataAnonymisationService).registerDataAnonymisation(userAccount, event2, isManuallyRequested);
        verify(dataAnonymisationService).registerDataAnonymisation(userAccount, event3, isManuallyRequested);
    }

    @ParameterizedTest(name = "Register data anonymisation event with isManuallyRequested = {0}")
    @ValueSource(booleans = {true, false})
    void registerDataAnonymisation_event_typical(boolean isManuallyRequested) {
        UserAccountEntity userAccount = mock(UserAccountEntity.class);
        EventEntity eventEntity = mock(EventEntity.class);
        OffsetDateTime currentTime = OffsetDateTime.now();
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(currentTime);
        dataAnonymisationService.registerDataAnonymisation(userAccount, eventEntity, isManuallyRequested);

        ArgumentCaptor<DataAnonymisationEntity> dataAnonymisationEntityArgumentCaptor = ArgumentCaptor.forClass(DataAnonymisationEntity.class);
        verify(dataAnonymisationRepository).save(dataAnonymisationEntityArgumentCaptor.capture());
        DataAnonymisationEntity dataAnonymisationEntity = dataAnonymisationEntityArgumentCaptor.getValue();
        assertThat(dataAnonymisationEntity.getEvent()).isEqualTo(eventEntity);
        assertThat(dataAnonymisationEntity.getTranscriptionComment()).isNull();
        assertThat(dataAnonymisationEntity.getIsManualRequest()).isEqualTo(isManuallyRequested);
        assertThat(dataAnonymisationEntity.getRequestedBy()).isEqualTo(userAccount);
        assertThat(dataAnonymisationEntity.getRequestedTs()).isEqualTo(currentTime);
        assertThat(dataAnonymisationEntity.getApprovedBy()).isEqualTo(userAccount);
        assertThat(dataAnonymisationEntity.getApprovedTs()).isEqualTo(currentTime);
        verify(currentTimeHelper, times(1)).currentOffsetDateTime();
    }

    @ParameterizedTest(name = "Register data anonymisation transcription comment with isManuallyRequested = {0}")
    @ValueSource(booleans = {true, false})
    void registerDataAnonymisation_transcriptionComment_typical(boolean isManuallyRequested) {
        UserAccountEntity userAccount = mock(UserAccountEntity.class);
        TranscriptionCommentEntity transcriptionCommentEntity = mock(TranscriptionCommentEntity.class);
        OffsetDateTime currentTime = OffsetDateTime.now();
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(currentTime);
        dataAnonymisationService.registerDataAnonymisation(userAccount, transcriptionCommentEntity, isManuallyRequested);

        ArgumentCaptor<DataAnonymisationEntity> dataAnonymisationEntityArgumentCaptor = ArgumentCaptor.forClass(DataAnonymisationEntity.class);
        verify(dataAnonymisationRepository).save(dataAnonymisationEntityArgumentCaptor.capture());
        DataAnonymisationEntity dataAnonymisationEntity = dataAnonymisationEntityArgumentCaptor.getValue();
        assertThat(dataAnonymisationEntity.getEvent()).isNull();
        assertThat(dataAnonymisationEntity.getTranscriptionComment()).isEqualTo(transcriptionCommentEntity);
        assertThat(dataAnonymisationEntity.getIsManualRequest()).isEqualTo(isManuallyRequested);
        assertThat(dataAnonymisationEntity.getRequestedBy()).isEqualTo(userAccount);
        assertThat(dataAnonymisationEntity.getRequestedTs()).isEqualTo(currentTime);
        assertThat(dataAnonymisationEntity.getApprovedBy()).isEqualTo(userAccount);
        assertThat(dataAnonymisationEntity.getApprovedTs()).isEqualTo(currentTime);
        verify(currentTimeHelper, times(1)).currentOffsetDateTime();
    }
}