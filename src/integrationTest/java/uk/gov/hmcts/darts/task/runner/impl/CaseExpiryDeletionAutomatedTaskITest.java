package uk.gov.hmcts.darts.task.runner.impl;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
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
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.retention.enums.RetentionPolicyEnum;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.test.common.data.DefenceTestData;
import uk.gov.hmcts.darts.test.common.data.DefendantTestData;
import uk.gov.hmcts.darts.test.common.data.ProsecutorTestData;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.EventLinkedCaseStub;
import uk.gov.hmcts.darts.testutils.stubs.EventStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.within;

@DisplayName("CaseExpiryDeletionAutomatedTask test")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class CaseExpiryDeletionAutomatedTaskITest extends PostgresIntegrationBase {

    private static final Pattern UUID_REGEX = Pattern.compile(TestUtils.UUID_REGEX);
    private static final int AUTOMATION_USER_ID = 0;

    private final CaseExpiryDeletionAutomatedTask caseExpiryDeletionAutomatedTask;
    private final EventLinkedCaseStub eventLinkedCaseStub;
    private int caseIndex;

    @Test
    void positiveRetentionDatePassed() {
        CourtCaseEntity courtCase = createCase(-1, CaseRetentionStatus.COMPLETE);
        final int caseId1 = courtCase.getId();

        caseExpiryDeletionAutomatedTask.preRunTask();
        caseExpiryDeletionAutomatedTask.runTask();

        assertCase(caseId1, true);
    }


    @Test
    void positiveRetentionDateNotPassed() {
        CourtCaseEntity courtCase = createCase(1, CaseRetentionStatus.COMPLETE);
        final int caseId1 = courtCase.getId();

        caseExpiryDeletionAutomatedTask.preRunTask();
        caseExpiryDeletionAutomatedTask.runTask();

        assertCase(caseId1, false);
    }

    @Test
    void positiveRetentionDatePassedByRetentionNotComplete() {
        CourtCaseEntity courtCase = createCase(-1, CaseRetentionStatus.PENDING);
        final int caseId1 = courtCase.getId();

        caseExpiryDeletionAutomatedTask.preRunTask();
        caseExpiryDeletionAutomatedTask.runTask();

        assertCase(caseId1, false);
    }

    @Test
    void positiveMultipleToAnonymiseAndSomeNotTo() {
        final CourtCaseEntity courtCase1 = createCase(-1, CaseRetentionStatus.COMPLETE);
        final CourtCaseEntity courtCase2 = createCase(-1, CaseRetentionStatus.COMPLETE);
        final CourtCaseEntity courtCase3 = createCase(-1, CaseRetentionStatus.COMPLETE);

        final CourtCaseEntity courtCase4 = createCase(-1, CaseRetentionStatus.PENDING);
        final CourtCaseEntity courtCase5 = createCase(1, CaseRetentionStatus.COMPLETE);

        caseExpiryDeletionAutomatedTask.preRunTask();
        caseExpiryDeletionAutomatedTask.runTask();

        assertCase(courtCase1.getId(), true);
        assertCase(courtCase2.getId(), true);
        assertCase(courtCase3.getId(), true);


        assertCase(courtCase4.getId(), false);
        assertCase(courtCase5.getId(), false);
    }


    private void assertCase(int caseId, boolean isAnonymised) {
        transactionalUtil.executeInTransaction(() -> {
            CourtCaseEntity courtCase = dartsDatabase.getCourtCaseStub().getCourtCase(caseId);
            assertThat(courtCase.isDataAnonymised())
                .isEqualTo(isAnonymised);

            if (isAnonymised) {
                assertThat(courtCase.getDataAnonymisedBy())
                    .isEqualTo(AUTOMATION_USER_ID);
                assertThat(courtCase.getDataAnonymisedTs())
                    .isCloseTo(OffsetDateTime.now(), within(10, ChronoUnit.SECONDS));
            } else {
                assertThat(courtCase.getDataAnonymisedBy())
                    .isNull();
                assertThat(courtCase.getDataAnonymisedTs())
                    .isNull();
            }

            assertThat(courtCase.getDefendantList()).hasSizeGreaterThan(0);
            assertThat(courtCase.getDefenceList()).hasSizeGreaterThan(0);
            assertThat(courtCase.getProsecutorList()).hasSizeGreaterThan(0);
            assertThat(courtCase.getHearings()).hasSizeGreaterThan(0);

            courtCase.getDefendantList().forEach(defendantEntity -> assertDefendant(defendantEntity, isAnonymised));
            courtCase.getDefenceList().forEach(defenceEntity -> assertDefence(defenceEntity, isAnonymised));
            courtCase.getProsecutorList().forEach(prosecutorEntity -> assertProsecutor(prosecutorEntity, isAnonymised));

            courtCase.getHearings().forEach(hearingEntity -> assertHearing(hearingEntity, isAnonymised));

            dartsDatabase.getEventRepository().findAllByCaseId(caseId).forEach(eventEntity -> assertEvent(eventEntity, isAnonymized));

            assertAuditEntries(courtCase, isAnonymised);
        });
    }

    private void assertAuditEntries(CourtCaseEntity courtCase, boolean isAnonymised) {
        List<AuditEntity> caseExpiredAuditEntries = dartsDatabase.getAuditRepository()
            .getAuditEntitiesByCaseAndActivityForDateRange(
                courtCase.getId(),
                AuditActivity.CASE_EXPIRED.getId(),
                OffsetDateTime.now().minusMinutes(1),
                OffsetDateTime.now().plusMinutes(1)
            );
        if (isAnonymised) {
            assertThat(caseExpiredAuditEntries).hasSize(1);
        } else {
            assertThat(caseExpiredAuditEntries).isEmpty();
        }
    }


    private void assertHearing(HearingEntity hearingEntity, boolean isAnonymised) {
        assertThat(hearingEntity.getTranscriptions()).hasSizeGreaterThan(0);
        assertThat(hearingEntity.getMediaRequests()).hasSizeGreaterThan(0);
        assertThat(hearingEntity.getEventList()).hasSizeGreaterThan(0);
        hearingEntity.getTranscriptions().forEach(transcriptionEntity -> assertTranscription(transcriptionEntity, isAnonymised));
        hearingEntity.getMediaRequests().forEach(mediaRequestEntity -> assertMediaRequest(mediaRequestEntity, isAnonymised));
        hearingEntity.getEventList().forEach(eventEntity -> assertEvent(eventEntity, isAnonymised));
    }

    private void assertMediaRequest(MediaRequestEntity mediaRequestEntity, boolean isAnonymised) {
        if (isAnonymised) {
            assertThat(mediaRequestEntity.getStatus()).isEqualTo(MediaRequestStatus.EXPIRED);
            assertThat(mediaRequestEntity.getTransformedMediaEntities()).isEmpty();
        } else {
            assertThat(mediaRequestEntity.getStatus()).isNotEqualTo(MediaRequestStatus.EXPIRED);
            assertThat(mediaRequestEntity.getTransformedMediaEntities()).isNotEmpty();
        }
    }

    private void assertTranscription(TranscriptionEntity transcriptionEntity, boolean isAnonymised) {
        assertThat(transcriptionEntity.getTranscriptionCommentEntities()).hasSizeGreaterThan(0);
        assertThat(transcriptionEntity.getTranscriptionWorkflowEntities()).hasSizeGreaterThan(0);
        transcriptionEntity.getTranscriptionCommentEntities().forEach(
            transcriptionCommentEntity -> assertTranscriptionComment(transcriptionCommentEntity, isAnonymised));
        transcriptionEntity.getTranscriptionWorkflowEntities().forEach(
            transcriptionWorkflowEntity -> assertTranscriptionWorkflowEntities(transcriptionWorkflowEntity, isAnonymised));

    }

    private void assertTranscriptionWorkflowEntities(TranscriptionWorkflowEntity transcriptionWorkflowEntity, boolean isAnonymised) {
        if (isAnonymised) {
            assertThat(transcriptionWorkflowEntity.getTranscriptionStatus().getId()).isEqualTo(TranscriptionStatusEnum.CLOSED.getId());
        } else {
            assertThat(transcriptionWorkflowEntity.getTranscriptionStatus().getId()).isNotEqualTo(TranscriptionStatusEnum.CLOSED.getId());
        }
    }

    private void assertEvent(EventEntity eventEntity, boolean isAnonymised) {
        if (isAnonymised) {
            assertThat(eventEntity.isDataAnonymised()).isTrue();
            assertThat(eventEntity.getEventText()).matches(UUID_REGEX);
        } else {
            assertThat(eventEntity.isDataAnonymised()).isFalse();
            assertThat(eventEntity.getEventText()).doesNotMatch(UUID_REGEX);
        }
    }

    private void assertTranscriptionComment(TranscriptionCommentEntity transcriptionCommentEntity, boolean isAnonymised) {
        if (isAnonymised) {
            assertThat(transcriptionCommentEntity.isDataAnonymised()).isTrue();
            assertThat(transcriptionCommentEntity.getComment()).matches(UUID_REGEX);
        } else {
            assertThat(transcriptionCommentEntity.isDataAnonymised()).isFalse();
            assertThat(transcriptionCommentEntity.getComment()).doesNotMatch(UUID_REGEX);
        }
    }

    private void assertDefendant(DefendantEntity defendantEntity, boolean isAnonymised) {
        if (isAnonymised) {
            assertThat(defendantEntity.getName()).matches(UUID_REGEX);
            assertThat(defendantEntity.getLastModifiedBy().getId()).isEqualTo(AUTOMATION_USER_ID);
        } else {
            assertThat(defendantEntity.getName()).doesNotMatch(UUID_REGEX);
            assertThat(defendantEntity.getLastModifiedBy().getId()).isNotEqualTo(AUTOMATION_USER_ID);
        }
    }

    private void assertDefence(DefenceEntity defenceEntity, boolean isAnonymised) {
        if (isAnonymised) {
            assertThat(defenceEntity.getName()).matches(UUID_REGEX);
            assertThat(defenceEntity.getLastModifiedBy().getId()).isEqualTo(AUTOMATION_USER_ID);
        } else {
            assertThat(defenceEntity.getName()).doesNotMatch(UUID_REGEX);
            assertThat(defenceEntity.getLastModifiedBy().getId()).isNotEqualTo(AUTOMATION_USER_ID);
        }
    }

    private void assertProsecutor(ProsecutorEntity prosecutorEntity, boolean isAnonymised) {
        if (isAnonymised) {
            assertThat(prosecutorEntity.getName()).matches(UUID_REGEX);
            assertThat(prosecutorEntity.getLastModifiedBy().getId()).isEqualTo(AUTOMATION_USER_ID);
        } else {
            assertThat(prosecutorEntity.getName()).doesNotMatch(UUID_REGEX);
            assertThat(prosecutorEntity.getLastModifiedBy().getId()).isNotEqualTo(AUTOMATION_USER_ID);
        }
    }


    private CourtCaseEntity createCase(final long daysUntilRetention, final CaseRetentionStatus caseRetentionStatus) {
        return transactionalUtil.executeInTransaction(() -> {
            caseIndex++;
            CourtCaseEntity caseEntity = dartsDatabase.createCase("Bristol", "case" + caseIndex);
            caseEntity.addDefendant(createDefendantEntity(caseEntity));
            caseEntity.addDefendant(createDefendantEntity(caseEntity));
            caseEntity.addDefendant(createDefendantEntity(caseEntity));

            caseEntity.addDefence(createDefenceEntity(caseEntity));
            caseEntity.addDefence(createDefenceEntity(caseEntity));
            caseEntity.addDefence(createDefenceEntity(caseEntity));

            caseEntity.addProsecutor(createProsecutorEntity(caseEntity));
            caseEntity.addProsecutor(createProsecutorEntity(caseEntity));
            caseEntity.addProsecutor(createProsecutorEntity(caseEntity));


            HearingEntity hearing = createHearing(caseEntity);
            createHearing(caseEntity);

            caseEntity = dartsDatabase.getCaseRepository().save(caseEntity);

            EventEntity event1 = dartsDatabase.getEventStub()
                .createEvent(hearing.getCourtroom(), 10, EventStub.STARTED_AT, "LOG", 2);
            eventLinkedCaseStub.createCaseLinkedEvent(event1, caseEntity.getCaseNumber(), caseEntity.getCourthouse().getCourthouseName());

            EventEntity event2 = dartsDatabase.getEventStub()
                .createEvent(hearing.getCourtroom(), 10, EventStub.STARTED_AT, "LOG", 2);
            eventLinkedCaseStub.createCaseLinkedEvent(event2, caseEntity);

            dartsDatabase.createCaseRetentionObject(
                null, caseEntity,
                OffsetDateTime.now().plusDays(daysUntilRetention),
                dartsDatabase.getRetentionPolicyTypeEntity(
                    RetentionPolicyEnum.DEFAULT),
                caseRetentionStatus.name(), true);
            return dartsDatabase.getCaseRepository().save(caseEntity);
        });
    }

    private HearingEntity createHearing(CourtCaseEntity caseEntity) {
        HearingEntity hearingEntity = dartsDatabase.getHearingStub()
            .createHearing("Bristol", "2", caseEntity.getCaseNumber(), DateConverterUtil.toLocalDateTime(EventStub.STARTED_AT));

        createTranscription(hearingEntity);
        createTranscription(hearingEntity);

        createMediaRequest(hearingEntity);
        createMediaRequest(hearingEntity);

        caseEntity.getHearings().add(hearingEntity);
        return hearingEntity;
    }

    private void createMediaRequest(HearingEntity hearingEntity) {
        MediaRequestEntity mediaRequestEntity = dartsDatabase.getMediaRequestStub()
            .createAndSaveMediaRequestEntity(hearingEntity.getCreatedBy(), hearingEntity);
        createTransformedMediaEntry(mediaRequestEntity);
        createTransformedMediaEntry(mediaRequestEntity);
    }

    private void createTransformedMediaEntry(MediaRequestEntity mediaRequestEntity) {
        TransformedMediaEntity transformedMediaEntity = dartsDatabase.getTransformedMediaStub()
            .createTransformedMediaEntity(mediaRequestEntity);
        createTransientObjectDirectoryEntity(transformedMediaEntity);
        createTransientObjectDirectoryEntity(transformedMediaEntity);
    }

    private void createTransientObjectDirectoryEntity(TransformedMediaEntity transformedMediaEntity) {
        dartsDatabase.getTransientObjectDirectoryStub()
            .createTransientObjectDirectoryEntity(
                transformedMediaEntity,
                dartsDatabase.getObjectRecordStatusEntity(ObjectRecordStatusEnum.STORED),
                UUID.randomUUID()
            );
    }

    private void createTranscription(HearingEntity hearingEntity) {
        TranscriptionStub transcriptionStub = dartsDatabase.getTranscriptionStub();
        TranscriptionEntity transcription = transcriptionStub
            .createTranscription(hearingEntity, hearingEntity.getCreatedBy());

        var transcriptionWorkflow1 = dartsDatabase.getTranscriptionStub()
            .createAndSaveTranscriptionWorkflow(
                transcription,
                OffsetDateTime.of(2024, 4, 23, 10, 0, 0, 0, ZoneOffset.UTC),
                transcriptionStub.getTranscriptionStatusByEnum(
                    TranscriptionStatusEnum.REQUESTED));
        transcriptionStub.createAndSaveTranscriptionWorkflowComment(transcriptionWorkflow1, "comment1", transcription.getCreatedBy());
        transcriptionStub.createAndSaveTranscriptionWorkflowComment(transcriptionWorkflow1, "comment2", transcription.getCreatedBy());
    }

    private DefendantEntity createDefendantEntity(CourtCaseEntity caseEntity) {
        DefendantEntity defendantEntity = DefendantTestData.createDefendantForCase(caseEntity);
        createUsers(defendantEntity);
        return defendantEntity;
    }

    private ProsecutorEntity createProsecutorEntity(CourtCaseEntity caseEntity) {
        ProsecutorEntity prosecutorEntity = ProsecutorTestData.createProsecutorForCase(caseEntity);
        createUsers(prosecutorEntity);
        return prosecutorEntity;
    }

    private DefenceEntity createDefenceEntity(CourtCaseEntity caseEntity) {
        DefenceEntity defenceEntity = DefenceTestData.createDefenceForCase(caseEntity);
        createUsers(defenceEntity);
        return defenceEntity;
    }

    private void createUsers(CreatedModifiedBaseEntity createdModifiedBaseEntity) {
        UserAccountEntity automationUser = dartsDatabase.getUserAccountRepository().findById(AUTOMATION_USER_ID).orElseThrow();
        UserAccountEntity createdBy = createdModifiedBaseEntity.getCreatedBy();
        createdBy.setCreatedBy(automationUser);
        createdBy.setLastModifiedBy(automationUser);
        createdModifiedBaseEntity.setCreatedBy(dartsDatabase.getUserAccountRepository().save(createdModifiedBaseEntity.getCreatedBy()));

        UserAccountEntity lastModifiedBy = createdModifiedBaseEntity.getLastModifiedBy();
        lastModifiedBy.setCreatedBy(automationUser);
        lastModifiedBy.setLastModifiedBy(automationUser);
        createdModifiedBaseEntity.setLastModifiedBy(dartsDatabase.getUserAccountRepository().save(createdModifiedBaseEntity.getLastModifiedBy()));
    }
}