package uk.gov.hmcts.darts.common.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
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
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.service.DataAnonymisationServiceImpl;
import uk.gov.hmcts.darts.test.common.TestUtils;

import java.util.List;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DataAnonymisationServiceImplTest {

    @Mock
    private AuditApi auditApi;
    @Mock
    private UserIdentity userIdentity;
    @InjectMocks
    @Spy
    private DataAnonymisationServiceImpl dataAnonymisationService;


    @Test
    void positiveEventEntityAnonymizeIsAutomatic() {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventText("event text");

        UserAccountEntity userAccount = new UserAccountEntity();
        dataAnonymisationService.anonymizeEventEntity(userAccount, eventEntity, false);
        assertThat(eventEntity.getEventText()).matches(TestUtils.UUID_REGEX);
        assertThat(eventEntity.isDataAnonymised()).isFalse();//This is only set for manual anonymization
        assertThat(eventEntity.getLastModifiedBy()).isEqualTo(userAccount);
    }

    @Test
    void positiveEventEntityAnonymizeIsManual() {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventText("event text");

        UserAccountEntity userAccount = new UserAccountEntity();
        dataAnonymisationService.anonymizeEventEntity(userAccount, eventEntity, true);
        assertThat(eventEntity.getEventText()).matches(TestUtils.UUID_REGEX);
        assertThat(eventEntity.isDataAnonymised()).isTrue();
        assertThat(eventEntity.getLastModifiedBy()).isEqualTo(userAccount);
    }

    @Test
    void positiveAnonymizeDefenceEntity() {
        DefenceEntity defenceEntity = new DefenceEntity();
        defenceEntity.setName("name");

        UserAccountEntity userAccount = new UserAccountEntity();

        dataAnonymisationService.anonymizeDefenceEntity(userAccount, defenceEntity);
        assertThat(defenceEntity.getName()).matches(TestUtils.UUID_REGEX);
        assertThat(defenceEntity.getLastModifiedBy()).isEqualTo(userAccount);
    }

    @Test
    void positiveAnonymizeDefendantEntity() {
        DefendantEntity defendantEntity = new DefendantEntity();
        defendantEntity.setName("name");

        UserAccountEntity userAccount = new UserAccountEntity();
        dataAnonymisationService.anonymizeDefendantEntity(userAccount, defendantEntity);
        assertThat(defendantEntity.getName()).matches(TestUtils.UUID_REGEX);
        assertThat(defendantEntity.getLastModifiedBy()).isEqualTo(userAccount);
    }

    @Test
    void positiveAnonymizeProsecutorEntity() {
        ProsecutorEntity prosecutorEntity = new ProsecutorEntity();
        prosecutorEntity.setName("name");

        UserAccountEntity userAccount = new UserAccountEntity();
        dataAnonymisationService.anonymizeProsecutorEntity(userAccount, prosecutorEntity);

        assertThat(prosecutorEntity.getName()).matches(TestUtils.UUID_REGEX);
        assertThat(prosecutorEntity.getLastModifiedBy()).isEqualTo(userAccount);
    }

    @Test
    void positiveAnonymizeTranscriptionCommentEntityIsAutomatic() {
        TranscriptionCommentEntity transcriptionCommentEntity = new TranscriptionCommentEntity();
        transcriptionCommentEntity.setComment("comment");

        UserAccountEntity userAccount = new UserAccountEntity();
        dataAnonymisationService.anonymizeTranscriptionCommentEntity(userAccount, transcriptionCommentEntity, false);
        assertThat(transcriptionCommentEntity.getComment()).matches(TestUtils.UUID_REGEX);
        assertThat(transcriptionCommentEntity.getLastModifiedBy()).isEqualTo(userAccount);
        assertThat(transcriptionCommentEntity.isDataAnonymised()).isFalse();//This is only set for manual anonymization
        assertThat(transcriptionCommentEntity.getLastModifiedBy()).isEqualTo(userAccount);
    }

    @Test
    void positiveAnonymizeTranscriptionCommentEntityIsManual() {
        TranscriptionCommentEntity transcriptionCommentEntity = new TranscriptionCommentEntity();
        transcriptionCommentEntity.setComment("comment");

        UserAccountEntity userAccount = new UserAccountEntity();
        dataAnonymisationService.anonymizeTranscriptionCommentEntity(userAccount, transcriptionCommentEntity, true);
        assertThat(transcriptionCommentEntity.getComment()).matches(TestUtils.UUID_REGEX);
        assertThat(transcriptionCommentEntity.getLastModifiedBy()).isEqualTo(userAccount);
        assertThat(transcriptionCommentEntity.isDataAnonymised()).isTrue();//This is only set for manual anonymization
        assertThat(transcriptionCommentEntity.getLastModifiedBy()).isEqualTo(userAccount);
    }

    @Test
    void positiveAnonymizeCourtCaseEntityIsAutomatic() {
        assertPositiveAnonymizeCourtCaseEntity(false);
    }

    @Test
    void positiveAnonymizeCourtCaseEntityIsManual() {
        assertPositiveAnonymizeCourtCaseEntity(true);
    }

    void assertPositiveAnonymizeCourtCaseEntity(boolean isManual) {
        CourtCaseEntity courtCase = new CourtCaseEntity();

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

        doNothing().when(dataAnonymisationService).anonymizeDefendantEntity(any(), any());
        doNothing().when(dataAnonymisationService).anonymizeDefenceEntity(any(), any());
        doNothing().when(dataAnonymisationService).anonymizeProsecutorEntity(any(), any());
        doNothing().when(dataAnonymisationService).anonymizeHearingEntity(any(), any(), anyBoolean());

        dataAnonymisationService.anonymizeCourtCaseEntity(userAccount, courtCase, isManual);

        assertThat(courtCase.isDataAnonymised()).isTrue();
        assertThat(courtCase.getDataAnonymisedBy()).isEqualTo(123);
        assertThat(courtCase.getDataAnonymisedTs()).isCloseToUtcNow(within(5, SECONDS));


        verify(dataAnonymisationService, times(1)).anonymizeDefendantEntity(userAccount, defendantEntity1);
        verify(dataAnonymisationService, times(1)).anonymizeDefendantEntity(userAccount, defendantEntity2);

        verify(dataAnonymisationService, times(1)).anonymizeDefenceEntity(userAccount, defenceEntity1);
        verify(dataAnonymisationService, times(1)).anonymizeDefenceEntity(userAccount, defenceEntity2);

        verify(dataAnonymisationService, times(1)).anonymizeProsecutorEntity(userAccount, prosecutorEntity1);
        verify(dataAnonymisationService, times(1)).anonymizeProsecutorEntity(userAccount, prosecutorEntity2);

        verify(dataAnonymisationService, times(1)).anonymizeHearingEntity(userAccount, hearingEntity1, isManual);
        verify(dataAnonymisationService, times(1)).anonymizeHearingEntity(userAccount, hearingEntity2, isManual);
    }


    @Test
    void positiveAnonymizeTranscriptionEntityIsManual() {
        positiveAnonymizeTranscriptionEntity(true);
    }


    @Test
    void positiveAnonymizeTranscriptionEntityIsAutomatic() {
        positiveAnonymizeTranscriptionEntity(false);

    }

    void positiveAnonymizeTranscriptionEntity(boolean isManual) {
        TranscriptionEntity transcriptionEntity = new TranscriptionEntity();

        TranscriptionCommentEntity transcriptionCommentEntity1 = mock(TranscriptionCommentEntity.class);
        TranscriptionCommentEntity transcriptionCommentEntity2 = mock(TranscriptionCommentEntity.class);
        transcriptionEntity.setTranscriptionCommentEntities(List.of(transcriptionCommentEntity1, transcriptionCommentEntity2));

        TranscriptionWorkflowEntity transcriptionWorkflowEntity1 = mock(TranscriptionWorkflowEntity.class);
        TranscriptionWorkflowEntity transcriptionWorkflowEntity2 = mock(TranscriptionWorkflowEntity.class);
        transcriptionEntity.setTranscriptionWorkflowEntities(List.of(transcriptionWorkflowEntity1, transcriptionWorkflowEntity2));

        doNothing().when(dataAnonymisationService).anonymizeTranscriptionCommentEntity(any(), any(), anyBoolean());
        doNothing().when(dataAnonymisationService).anonymizeTranscriptionWorkflowEntity(any());


        UserAccountEntity userAccount = new UserAccountEntity();
        dataAnonymisationService.anonymizeTranscriptionEntity(userAccount, transcriptionEntity, isManual);

        verify(dataAnonymisationService, times(1)).anonymizeTranscriptionCommentEntity(userAccount, transcriptionCommentEntity1, isManual);
        verify(dataAnonymisationService, times(1)).anonymizeTranscriptionCommentEntity(userAccount, transcriptionCommentEntity2, isManual);

        verify(dataAnonymisationService, times(1)).anonymizeTranscriptionWorkflowEntity(transcriptionWorkflowEntity1);
        verify(dataAnonymisationService, times(1)).anonymizeTranscriptionWorkflowEntity(transcriptionWorkflowEntity2);
    }

    @Test
    void positiveAnonymizeHearingEntityIsAutomatic() {
        positiveAnonymizeHearingEntity(false);
    }

    @Test
    void positiveAnonymizeHearingEntityIsManual() {
        positiveAnonymizeHearingEntity(true);
    }

    void positiveAnonymizeHearingEntity(boolean isManual) {
        HearingEntity hearingEntity = new HearingEntity();

        TranscriptionEntity transcriptionEntity1 = mock(TranscriptionEntity.class);
        TranscriptionEntity transcriptionEntity2 = mock(TranscriptionEntity.class);
        hearingEntity.setTranscriptions(List.of(transcriptionEntity1, transcriptionEntity2));

        EventEntity entityEntity1 = mock(EventEntity.class);
        EventEntity entityEntity2 = mock(EventEntity.class);
        hearingEntity.setEventList(List.of(entityEntity1, entityEntity2));

        doNothing().when(dataAnonymisationService).anonymizeTranscriptionEntity(any(), any(), anyBoolean());
        doNothing().when(dataAnonymisationService).anonymizeEventEntity(any(), any(), anyBoolean());

        UserAccountEntity userAccount = new UserAccountEntity();
        dataAnonymisationService.anonymizeHearingEntity(userAccount, hearingEntity, isManual);

        verify(dataAnonymisationService, times(1)).anonymizeTranscriptionEntity(userAccount, transcriptionEntity1, isManual);
        verify(dataAnonymisationService, times(1)).anonymizeTranscriptionEntity(userAccount, transcriptionEntity2, isManual);

        verify(dataAnonymisationService, times(1)).anonymizeEventEntity(userAccount, entityEntity1, isManual);
        verify(dataAnonymisationService, times(1)).anonymizeEventEntity(userAccount, entityEntity2, isManual);

    }
}
