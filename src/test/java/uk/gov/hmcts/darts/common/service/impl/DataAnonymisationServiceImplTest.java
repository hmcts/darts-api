package uk.gov.hmcts.darts.common.service.impl;

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
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
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
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.event.service.EventService;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.test.common.TestUtils;

import java.time.OffsetDateTime;
import java.util.List;

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
    private UserIdentity userIdentity;

    @Mock
    private CurrentTimeHelper currentTimeHelper;
    @Mock
    private ExternalOutboundDataStoreDeleter outboundDataStoreDeleter;
    @Mock
    private CaseRepository caseRepository;
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
    void positiveEventEntityAnonymize() {
        setupOffsetDateTime();
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventText("event text");

        UserAccountEntity userAccount = new UserAccountEntity();
        dataAnonymisationService.anonymizeEventEntity(userAccount, eventEntity);
        assertThat(eventEntity.getEventText()).matches(TestUtils.UUID_REGEX);
        assertThat(eventEntity.isDataAnonymised()).isTrue();
        assertLastModifiedByAndAt(eventEntity, userAccount);
    }

    @Test
    void positiveAnonymizeDefenceEntity() {
        setupOffsetDateTime();
        DefenceEntity defenceEntity = new DefenceEntity();
        defenceEntity.setName("name");

        UserAccountEntity userAccount = new UserAccountEntity();

        dataAnonymisationService.anonymizeDefenceEntity(userAccount, defenceEntity);
        assertThat(defenceEntity.getName()).matches(TestUtils.UUID_REGEX);
        assertLastModifiedByAndAt(defenceEntity, userAccount);
    }

    @Test
    void positiveAnonymizeDefendantEntity() {
        setupOffsetDateTime();
        DefendantEntity defendantEntity = new DefendantEntity();
        defendantEntity.setName("name");

        UserAccountEntity userAccount = new UserAccountEntity();
        dataAnonymisationService.anonymizeDefendantEntity(userAccount, defendantEntity);
        assertThat(defendantEntity.getName()).matches(TestUtils.UUID_REGEX);
        assertLastModifiedByAndAt(defendantEntity, userAccount);
    }

    @Test
    void positiveAnonymizeProsecutorEntity() {
        setupOffsetDateTime();
        ProsecutorEntity prosecutorEntity = new ProsecutorEntity();
        prosecutorEntity.setName("name");

        UserAccountEntity userAccount = new UserAccountEntity();
        dataAnonymisationService.anonymizeProsecutorEntity(userAccount, prosecutorEntity);

        assertThat(prosecutorEntity.getName()).matches(TestUtils.UUID_REGEX);
        assertLastModifiedByAndAt(prosecutorEntity, userAccount);
    }

    @Test
    void positiveAnonymizeTranscriptionCommentEntity() {
        setupOffsetDateTime();
        TranscriptionCommentEntity transcriptionCommentEntity = new TranscriptionCommentEntity();
        transcriptionCommentEntity.setComment("comment");

        UserAccountEntity userAccount = new UserAccountEntity();
        dataAnonymisationService.anonymizeTranscriptionCommentEntity(userAccount, transcriptionCommentEntity);
        assertThat(transcriptionCommentEntity.getComment()).matches(TestUtils.UUID_REGEX);
        assertThat(transcriptionCommentEntity.getLastModifiedBy()).isEqualTo(userAccount);
        assertThat(transcriptionCommentEntity.isDataAnonymised()).isTrue();
        assertLastModifiedByAndAt(transcriptionCommentEntity, userAccount);
    }


    @Test
    void assertPositiveAnonymizeCourtCaseEntity() {
        setupOffsetDateTime();
        CourtCaseEntity courtCase = new CourtCaseEntity();
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

        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(123);

        dataAnonymisationService.anonymizeCourtCaseEntity(userAccount, courtCase);

        assertThat(courtCase.isDataAnonymised()).isTrue();
        assertThat(courtCase.getDataAnonymisedBy()).isEqualTo(123);
        assertThat(courtCase.getDataAnonymisedTs()).isCloseToUtcNow(within(5, SECONDS));


        verify(dataAnonymisationService, times(1)).anonymizeDefendantEntity(userAccount, defendantEntity1);
        verify(dataAnonymisationService, times(1)).anonymizeDefendantEntity(userAccount, defendantEntity2);

        verify(dataAnonymisationService, times(1)).anonymizeDefenceEntity(userAccount, defenceEntity1);
        verify(dataAnonymisationService, times(1)).anonymizeDefenceEntity(userAccount, defenceEntity2);

        verify(dataAnonymisationService, times(1)).anonymizeProsecutorEntity(userAccount, prosecutorEntity1);
        verify(dataAnonymisationService, times(1)).anonymizeProsecutorEntity(userAccount, prosecutorEntity2);

        verify(dataAnonymisationService, times(1)).anonymizeTranscriptionEntities(userAccount, hearingEntity1);
        verify(dataAnonymisationService, times(1)).anonymizeTranscriptionEntities(userAccount, hearingEntity2);

        verify(dataAnonymisationService, times(1)).tidyUpTransformedMediaEntities(userAccount, courtCase);
        verify(logApi, times(1)).caseDeletedDueToExpiry(123, "caseNo123");

    }


    @Test
    void positiveAnonymizeTranscriptionEntity() {
        TranscriptionEntity transcriptionEntity = new TranscriptionEntity();

        TranscriptionCommentEntity transcriptionCommentEntity1 = mock(TranscriptionCommentEntity.class);
        TranscriptionCommentEntity transcriptionCommentEntity2 = mock(TranscriptionCommentEntity.class);
        transcriptionEntity.setTranscriptionCommentEntities(List.of(transcriptionCommentEntity1, transcriptionCommentEntity2));

        TranscriptionWorkflowEntity transcriptionWorkflowEntity1 = mock(TranscriptionWorkflowEntity.class);
        TranscriptionWorkflowEntity transcriptionWorkflowEntity2 = mock(TranscriptionWorkflowEntity.class);
        transcriptionEntity.setTranscriptionWorkflowEntities(List.of(transcriptionWorkflowEntity1, transcriptionWorkflowEntity2));

        doNothing().when(dataAnonymisationService).anonymizeTranscriptionCommentEntity(any(), any());
        doNothing().when(dataAnonymisationService).anonymizeTranscriptionWorkflowEntity(any());


        UserAccountEntity userAccount = new UserAccountEntity();
        dataAnonymisationService.anonymizeTranscriptionEntity(userAccount, transcriptionEntity);

        verify(dataAnonymisationService, times(1)).anonymizeTranscriptionCommentEntity(userAccount, transcriptionCommentEntity1);
        verify(dataAnonymisationService, times(1)).anonymizeTranscriptionCommentEntity(userAccount, transcriptionCommentEntity2);

        verify(dataAnonymisationService, times(1)).anonymizeTranscriptionWorkflowEntity(transcriptionWorkflowEntity1);
        verify(dataAnonymisationService, times(1)).anonymizeTranscriptionWorkflowEntity(transcriptionWorkflowEntity2);
    }

    @Test
    void positiveAnonymizeHearingEntity() {
        HearingEntity hearingEntity = new HearingEntity();

        TranscriptionEntity transcriptionEntity1 = mock(TranscriptionEntity.class);
        TranscriptionEntity transcriptionEntity2 = mock(TranscriptionEntity.class);
        hearingEntity.setTranscriptions(List.of(transcriptionEntity1, transcriptionEntity2));

        EventEntity entityEntity1 = mock(EventEntity.class);
        EventEntity entityEntity2 = mock(EventEntity.class);
        hearingEntity.setEventList(List.of(entityEntity1, entityEntity2));

        doNothing().when(dataAnonymisationService).anonymizeTranscriptionEntity(any(), any());

        UserAccountEntity userAccount = new UserAccountEntity();
        dataAnonymisationService.anonymizeTranscriptionEntities(userAccount, hearingEntity);

        verify(dataAnonymisationService, times(1)).anonymizeTranscriptionEntity(userAccount, transcriptionEntity1);
        verify(dataAnonymisationService, times(1)).anonymizeTranscriptionEntity(userAccount, transcriptionEntity2);
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
        when(outboundDataStoreDeleter.delete(any())).thenReturn(true);

        dataAnonymisationService.deleteTransientObjectDirectoryEntity(transientObjectDirectoryEntity);

        verify(outboundDataStoreDeleter, times(1)).delete(transientObjectDirectoryEntity);
        verify(transientObjectDirectoryRepository, times(1)).delete(transientObjectDirectoryEntity);
    }

    @Test
    void negativeDeleteTransientObjectDirectoryEntryFailure() {
        TransientObjectDirectoryEntity transientObjectDirectoryEntity = new TransientObjectDirectoryEntity();
        when(outboundDataStoreDeleter.delete(any())).thenReturn(false);

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
    void positiveObfuscateEventByIds() {
        EventEntity event1 = mock(EventEntity.class);
        EventEntity event2 = mock(EventEntity.class);
        EventEntity event3 = mock(EventEntity.class);


        doReturn(event1).when(eventService).getEventByEveId(1);
        doReturn(event2).when(eventService).getEventByEveId(2);
        doReturn(event3).when(eventService).getEventByEveId(3);
        doReturn(event1).when(eventService).getEventByEveId(4);

        dataAnonymisationService.obfuscateEventByIds(List.of(1, 2, 3, 4));


        verify(dataAnonymisationService, times(1)).anonymizeEvent(event1);
        verify(dataAnonymisationService, times(1)).anonymizeEvent(event2);
        verify(dataAnonymisationService, times(1)).anonymizeEvent(event3);

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