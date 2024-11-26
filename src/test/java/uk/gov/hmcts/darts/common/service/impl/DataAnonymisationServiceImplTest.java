package uk.gov.hmcts.darts.common.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.deleter.impl.outbound.ExternalOutboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

    @InjectMocks
    @Spy
    private DataAnonymisationServiceImpl dataAnonymisationService;


    private OffsetDateTime offsetDateTime;

    private void setupOffsetDateTime() {
        offsetDateTime = OffsetDateTime.now();
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(offsetDateTime);
    }

    private void assertLastModifiedByAndAt(CreatedModifiedBaseEntity entity, UserAccountEntity userAccount) {
        assertThat(entity.getLastModifiedBy()).isEqualTo(userAccount);
        assertThat(entity.getLastModifiedDateTime()).isEqualTo(offsetDateTime);
    }


    @Test
    @DisplayName("Event should not be anonymised if one or more assocaited cases are not anonymised")
    void eventEntityAnonymiseNotUpdatedAsNotAllCasesExpiredAndOnlyAnonymiseIfAllCasesExpiredIsTrue() {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventText("event text");

        UserAccountEntity userAccount = new UserAccountEntity();
        when(eventService.allAssociatedCasesAnonymised(eventEntity)).thenReturn(false);
        dataAnonymisationService.anonymiseEventEntity(userAccount, eventEntity, true);
        assertThat(eventEntity.getEventText()).isEqualTo("event text");
        assertThat(eventEntity.isDataAnonymised()).isFalse();
        verify(eventService).allAssociatedCasesAnonymised(eventEntity);
        verify(eventService, never()).saveEvent(eventEntity);
    }

    @Test
    void positiveEventEntityAnonymiseUpdatedAsAllCasesExpiredAndOnlyAnonymiseIfAllCasesExpiredIsTrue() {
        setupOffsetDateTime();
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventText("event text");

        UserAccountEntity userAccount = new UserAccountEntity();
        when(eventService.allAssociatedCasesAnonymised(eventEntity)).thenReturn(true);
        dataAnonymisationService.anonymiseEventEntity(userAccount, eventEntity, true);
        assertThat(eventEntity.getEventText()).matches(TestUtils.UUID_REGEX);
        assertThat(eventEntity.isDataAnonymised()).isTrue();
        assertLastModifiedByAndAt(eventEntity, userAccount);
        verify(eventService).allAssociatedCasesAnonymised(eventEntity);
        verify(eventService).saveEvent(eventEntity);
    }

    @Test
    @DisplayName("Event should be anonymised if one or more assocaited cases are not anonymised and the onlyAnonymiseIfAllCasesExpired flag is false")
    void eventEntityAnonymiseUpdatedAsNotAllCasesExpiredAndOnlyAnonymiseIfAllCasesExpiredIsFalse() {
        setupOffsetDateTime();
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventText("event text");

        UserAccountEntity userAccount = new UserAccountEntity();
        dataAnonymisationService.anonymiseEventEntity(userAccount, eventEntity, false);
        assertThat(eventEntity.getEventText()).matches(TestUtils.UUID_REGEX);
        assertThat(eventEntity.isDataAnonymised()).isTrue();
        assertLastModifiedByAndAt(eventEntity, userAccount);
        verify(eventService).saveEvent(eventEntity);
    }

    @Test
    @DisplayName("Event should not be anonymised again if the event is already anonymised")
    void eventEntityNotUpdatedAsAlreadyAnonymised() {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventText("event text");
        eventEntity.setDataAnonymised(true);

        UserAccountEntity userAccount = new UserAccountEntity();
        dataAnonymisationService.anonymiseEventEntity(userAccount, eventEntity, false);
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

    @Test
    void positiveAnonymiseTranscriptionCommentEntity() {
        setupOffsetDateTime();
        TranscriptionCommentEntity transcriptionCommentEntity = new TranscriptionCommentEntity();
        transcriptionCommentEntity.setComment("comment");

        UserAccountEntity userAccount = new UserAccountEntity();
        dataAnonymisationService.anonymiseTranscriptionCommentEntity(userAccount, transcriptionCommentEntity);
        assertThat(transcriptionCommentEntity.getComment()).matches(TestUtils.UUID_REGEX);
        assertThat(transcriptionCommentEntity.getLastModifiedBy()).isEqualTo(userAccount);
        assertThat(transcriptionCommentEntity.isDataAnonymised()).isTrue();
        assertLastModifiedByAndAt(transcriptionCommentEntity, userAccount);
    }


    @Test
    void assertPositiveAnonymiseCourtCaseEntity() {
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

        EventEntity entityEntity1 = mock(EventEntity.class);
        EventEntity entityEntity2 = mock(EventEntity.class);
        when(eventService.getAllCourtCaseEventVersions(courtCase)).thenReturn(Set.of(entityEntity1, entityEntity2));

        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(123);

        dataAnonymisationService.anonymiseCourtCaseById(userAccount, 123);
        assertThat(courtCase.isDataAnonymised()).isTrue();
        assertThat(courtCase.getDataAnonymisedBy()).isEqualTo(123);
        assertThat(courtCase.getDataAnonymisedTs()).isCloseToUtcNow(within(5, SECONDS));


        verify(dataAnonymisationService, times(1)).anonymiseDefendantEntity(userAccount, defendantEntity1);
        verify(dataAnonymisationService, times(1)).anonymiseDefendantEntity(userAccount, defendantEntity2);

        verify(dataAnonymisationService, times(1)).anonymiseDefenceEntity(userAccount, defenceEntity1);
        verify(dataAnonymisationService, times(1)).anonymiseDefenceEntity(userAccount, defenceEntity2);

        verify(dataAnonymisationService, times(1)).anonymiseProsecutorEntity(userAccount, prosecutorEntity1);
        verify(dataAnonymisationService, times(1)).anonymiseProsecutorEntity(userAccount, prosecutorEntity2);

        verify(dataAnonymisationService, times(1)).anonymiseHearingEntity(userAccount, hearingEntity1);
        verify(dataAnonymisationService, times(1)).anonymiseHearingEntity(userAccount, hearingEntity2);

        verify(dataAnonymisationService, times(1)).anonymiseEventEntity(userAccount, entityEntity1, true);
        verify(dataAnonymisationService, times(1)).anonymiseEventEntity(userAccount, entityEntity2, true);

        verify(dataAnonymisationService, times(1)).tidyUpTransformedMediaEntities(userAccount, courtCase);
        verify(caseService, times(1)).saveCase(courtCase);
        verify(logApi, times(1)).caseDeletedDueToExpiry(123, "caseNo123");
        verify(caseService, times(1)).getCourtCaseById(123);

    }


    @Test
    void positiveAnonymiseTranscriptionEntity() {
        TranscriptionEntity transcriptionEntity = new TranscriptionEntity();

        TranscriptionCommentEntity transcriptionCommentEntity1 = mock(TranscriptionCommentEntity.class);
        TranscriptionCommentEntity transcriptionCommentEntity2 = mock(TranscriptionCommentEntity.class);
        transcriptionEntity.setTranscriptionCommentEntities(List.of(transcriptionCommentEntity1, transcriptionCommentEntity2));

        TranscriptionWorkflowEntity transcriptionWorkflowEntity1 = mock(TranscriptionWorkflowEntity.class);
        TranscriptionWorkflowEntity transcriptionWorkflowEntity2 = mock(TranscriptionWorkflowEntity.class);
        transcriptionEntity.setTranscriptionWorkflowEntities(List.of(transcriptionWorkflowEntity1, transcriptionWorkflowEntity2));

        doNothing().when(dataAnonymisationService).anonymiseTranscriptionCommentEntity(any(), any());
        doNothing().when(dataAnonymisationService).anonymiseTranscriptionWorkflowEntity(any());


        UserAccountEntity userAccount = new UserAccountEntity();
        dataAnonymisationService.anonymiseTranscriptionEntity(userAccount, transcriptionEntity);

        verify(dataAnonymisationService, times(1)).anonymiseTranscriptionCommentEntity(userAccount, transcriptionCommentEntity1);
        verify(dataAnonymisationService, times(1)).anonymiseTranscriptionCommentEntity(userAccount, transcriptionCommentEntity2);

        verify(dataAnonymisationService, times(1)).anonymiseTranscriptionWorkflowEntity(transcriptionWorkflowEntity1);
        verify(dataAnonymisationService, times(1)).anonymiseTranscriptionWorkflowEntity(transcriptionWorkflowEntity2);
    }

    @Test
    void positiveAnonymiseHearingEntity() {
        HearingEntity hearingEntity = new HearingEntity();

        TranscriptionEntity transcriptionEntity1 = mock(TranscriptionEntity.class);
        TranscriptionEntity transcriptionEntity2 = mock(TranscriptionEntity.class);
        hearingEntity.setTranscriptions(List.of(transcriptionEntity1, transcriptionEntity2));

        EventEntity entityEntity1 = mock(EventEntity.class);
        EventEntity entityEntity2 = mock(EventEntity.class);
        hearingEntity.setEventList(List.of(entityEntity1, entityEntity2));

        doNothing().when(dataAnonymisationService).anonymiseTranscriptionEntity(any(), any());

        UserAccountEntity userAccount = new UserAccountEntity();
        dataAnonymisationService.anonymiseHearingEntity(userAccount, hearingEntity);

        verify(dataAnonymisationService, times(1)).anonymiseTranscriptionEntity(userAccount, transcriptionEntity1);
        verify(dataAnonymisationService, times(1)).anonymiseTranscriptionEntity(userAccount, transcriptionEntity2);
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

        verify(outboundDataStoreDeleter, times(1)).delete(transientObjectDirectoryEntity);
        verify(transientObjectDirectoryRepository, times(1)).delete(transientObjectDirectoryEntity);
    }

    @Test
    void negativeDeleteTransientObjectDirectoryEntryFailure() {
        TransientObjectDirectoryEntity transientObjectDirectoryEntity = new TransientObjectDirectoryEntity();
        when(outboundDataStoreDeleter.delete(any(TransientObjectDirectoryEntity.class))).thenReturn(false);

        dataAnonymisationService.deleteTransientObjectDirectoryEntity(transientObjectDirectoryEntity);

        verify(outboundDataStoreDeleter, times(1)).delete(transientObjectDirectoryEntity);
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

        verify(dataAnonymisationService, times(1)).deleteTransientObjectDirectoryEntity(transientObjectDirectoryEntity1);
        verify(dataAnonymisationService, times(1)).deleteTransientObjectDirectoryEntity(transientObjectDirectoryEntity2);
        verify(dataAnonymisationService, times(1)).deleteTransientObjectDirectoryEntity(transientObjectDirectoryEntity3);
        verify(transformedMediaRepository, times(1)).delete(transformedMediaEntity);
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

        verify(dataAnonymisationService, times(1)).expiredMediaRequest(userAccount, hearing1MediaRequestEntity1);
        verify(dataAnonymisationService, times(1)).expiredMediaRequest(userAccount, hearing1MediaRequestEntity2);
        verify(dataAnonymisationService, times(1)).expiredMediaRequest(userAccount, hearing1MediaRequestEntity3);


        verify(dataAnonymisationService, times(1)).expiredMediaRequest(userAccount, hearing2MediaRequestEntity1);
        verify(dataAnonymisationService, times(1)).expiredMediaRequest(userAccount, hearing2MediaRequestEntity2);
        verify(dataAnonymisationService, times(1)).expiredMediaRequest(userAccount, hearing2MediaRequestEntity3);

        verify(dataAnonymisationService, times(1)).expiredMediaRequest(userAccount, hearing3MediaRequestEntity1);


        verify(dataAnonymisationService, times(1))
            .deleteTransformedMediaEntity(transformedMediaEntity1);
        verify(dataAnonymisationService, times(1))
            .deleteTransformedMediaEntity(transformedMediaEntity2);
        verify(dataAnonymisationService, times(1))
            .deleteTransformedMediaEntity(transformedMediaEntity3);
        verify(dataAnonymisationService, times(1))
            .deleteTransformedMediaEntity(transformedMediaEntity4);
        verify(dataAnonymisationService, times(1))
            .deleteTransformedMediaEntity(transformedMediaEntity5);
    }

    @Test
    void positiveAnonymiseEventByIds() {
        EventEntity event1 = mock(EventEntity.class);
        EventEntity event2 = mock(EventEntity.class);
        EventEntity event3 = mock(EventEntity.class);


        doReturn(event1).when(eventService).getEventByEveId(1);
        doReturn(event2).when(eventService).getEventByEveId(2);
        doReturn(event3).when(eventService).getEventByEveId(3);
        doReturn(event1).when(eventService).getEventByEveId(4);
        UserAccountEntity userAccount = mock(UserAccountEntity.class);
        dataAnonymisationService.anonymiseEventByIds(userAccount, List.of(1, 2, 3, 4));


        verify(dataAnonymisationService, times(1)).anonymiseEventEntity(userAccount, event1, false);
        verify(dataAnonymisationService, times(1)).anonymiseEventEntity(userAccount, event2, false);
        verify(dataAnonymisationService, times(1)).anonymiseEventEntity(userAccount, event3, false);

        verify(eventService, times(1)).getEventByEveId(1);
        verify(eventService, times(1)).getEventByEveId(2);
        verify(eventService, times(1)).getEventByEveId(3);
        verify(eventService, times(1)).getEventByEveId(4);

        verify(eventService, times(1)).saveEvent(event1);
        verify(eventService, times(1)).saveEvent(event2);
        verify(eventService, times(1)).saveEvent(event3);
        verifyNoMoreInteractions(eventService);
    }
}