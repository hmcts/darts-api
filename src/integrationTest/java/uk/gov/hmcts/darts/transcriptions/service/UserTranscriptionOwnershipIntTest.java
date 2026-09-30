package uk.gov.hmcts.darts.transcriptions.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.TranscriptionWorkflowRepository;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.CLOSED_TRANSCRIPTION;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.CLOSED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;

class UserTranscriptionOwnershipIntTest extends IntegrationBase {

    @Autowired
    private GivenBuilder given;
    @Autowired
    private TranscriptionService transcriptionService;
    @Autowired
    private TranscriptionWorkflowRepository transcriptionWorkflowRepository;

    private HearingEntity hearing;

    @BeforeEach
    void setUp() {
        hearing = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            "CLOSE-TRANSCRIPTION-CASE",
            "CLOSE-TRANSCRIPTION-COURTHOUSE",
            "close-transcription-courtroom",
            LocalDateTime.now()
        );
    }

    @Test
    void closeUserTranscriptions_shouldLeaveReassignedWorkOpen_whenClosingPreviousOwnersWork() {
        UserAccountEntity previousOwner = createTranscriber("previous-owner");
        previousOwner.setActive(false);
        dartsPersistence.save(previousOwner);
        UserAccountEntity currentOwner = createTranscriber("current-owner");
        TranscriptionEntity transcription = createTranscriptionWithAssignmentHistory_lastUserIsCurrentOwner(previousOwner, currentOwner);
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        List<TranscriptionEntity> previousOwnersCurrentWork =
            transcriptionWorkflowRepository.findWorkflowForUserWithTranscriptionState(previousOwner.getId(), WITH_TRANSCRIBER.getId());
        assertTrue(previousOwnersCurrentWork.isEmpty());

        transcriptionService.closeUserTranscriptions(previousOwner, "Owner was disabled due to inactivity");

        TranscriptionEntity reloadedTranscription = dartsDatabase.getTranscriptionRepository()
            .findById(transcription.getId())
            .orElseThrow();
        assertEquals(WITH_TRANSCRIBER.getId(), reloadedTranscription.getTranscriptionStatus().getId());
        assertThat(dartsDatabase.findAudits())
            .noneMatch(audit -> CLOSED_TRANSCRIPTION.getId().equals(audit.getAuditActivity().getId()));
    }

    @Test
    void closeUserTranscriptions_shouldCloseWorkAndAuditEachTranscription_whenUserIsLatestTranscriber() {
        UserAccountEntity previousOwner = createTranscriber("previous-owner");
        UserAccountEntity currentOwner = createTranscriber("current-owner");
        TranscriptionEntity transcription = createTranscriptionWithAssignmentHistory_lastUserIsCurrentOwner(currentOwner, previousOwner);
        TranscriptionEntity secondTranscription = createTranscriptionWithAssignmentHistory_lastUserIsCurrentOwner(currentOwner, previousOwner);
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        transcriptionService.closeUserTranscriptions(previousOwner, "Owner was disabled due to inactivity");

        TranscriptionEntity reloadedTranscription = dartsDatabase.getTranscriptionRepository()
            .findById(transcription.getId())
            .orElseThrow();
        assertEquals(CLOSED.getId(), reloadedTranscription.getTranscriptionStatus().getId());
        TranscriptionEntity reloadedSecondTranscription = dartsDatabase.getTranscriptionRepository()
            .findById(secondTranscription.getId())
            .orElseThrow();
        assertEquals(CLOSED.getId(), reloadedSecondTranscription.getTranscriptionStatus().getId());

        List<AuditEntity> workflowAudits = dartsDatabase.findAudits().stream()
            .filter(audit -> CLOSED_TRANSCRIPTION.getId().equals(audit.getAuditActivity().getId()))
            .toList();
        assertThat(workflowAudits).hasSize(2);
        assertThat(workflowAudits).allSatisfy(workflowAudit -> assertThat(workflowAudit.getAdditionalData()).isNull());
    }

    @Test
    void rollbackUserTranscriptions_shouldLeaveReassignedWorkUnchanged_whenUserIsPreviousTranscriber() {
        UserAccountEntity previousOwner = createTranscriber("previous-owner");
        UserAccountEntity currentOwner = createTranscriber("current-owner");
        TranscriptionEntity transcription = createTranscriptionWithAssignmentHistory_lastUserIsCurrentOwner(previousOwner, currentOwner);

        List<Long> rolledBackTranscriptionIds = transcriptionService.rollbackUserTranscriptions(previousOwner);

        assertTrue(rolledBackTranscriptionIds.isEmpty());
        assertLatestWorkflow(transcription, currentOwner, WITH_TRANSCRIBER);
    }

    @Test
    void rollbackUserTranscriptions_shouldReturnWorkToApproved_whenUserIsLatestTranscriber() {
        UserAccountEntity previousOwner = createTranscriber("previous-owner");
        UserAccountEntity currentOwner = createTranscriber("current-owner");
        TranscriptionEntity transcription = createTranscriptionWithAssignmentHistory_lastUserIsCurrentOwner(currentOwner, previousOwner);

        List<Long> rolledBackTranscriptionIds = transcriptionService.rollbackUserTranscriptions(previousOwner);

        assertEquals(List.of(transcription.getId()), rolledBackTranscriptionIds);
        assertLatestWorkflow(transcription, previousOwner, APPROVED);
    }

    private void assertLatestWorkflow(TranscriptionEntity transcription, UserAccountEntity expectedOwner,
                                     TranscriptionStatusEnum expectedStatus) {
        List<TranscriptionWorkflowEntity> workflows =
            dartsDatabase.getTranscriptionWorkflowRepository()
                .findByTranscriptionOrderByWorkflowTimestampDesc(transcription);
        assertFalse(workflows.isEmpty());
        assertEquals(expectedOwner.getId(), workflows.getFirst().getWorkflowActor().getId());
        assertEquals(expectedStatus.getId(), workflows.getFirst().getTranscriptionStatus().getId());
    }

    private UserAccountEntity createTranscriber(String userName) {
        UserAccountEntity user = dartsDatabase.getUserAccountStub()
            .createAuthorisedIntegrationTestUser(false, hearing.getCourtroom().getCourthouse());
        user.setUserName(userName);
        return dartsPersistence.save(user);
    }

    private TranscriptionEntity createTranscriptionWithAssignmentHistory_lastUserIsCurrentOwner(UserAccountEntity... owners) {
        var transcriptionStub = dartsDatabase.getTranscriptionStub();
        TranscriptionEntity transcription = transcriptionStub.createTranscription(
            hearing,
            owners[0],
            APPROVED
        );
        var withTranscriberStatus = transcriptionStub.getTranscriptionStatusByEnum(WITH_TRANSCRIBER);
        OffsetDateTime workflowTimestamp = OffsetDateTime.now().minusSeconds(10);

        for (UserAccountEntity owner : owners) {
            TranscriptionWorkflowEntity workflow = transcriptionStub.createTranscriptionWorkflowEntity(
                transcription,
                owner,
                workflowTimestamp,
                withTranscriberStatus
            );
            transcription.getTranscriptionWorkflowEntities().add(workflow);
            dartsPersistence.save(workflow);
            workflowTimestamp = workflowTimestamp.plusSeconds(1);
        }

        transcription.setTranscriptionStatus(withTranscriberStatus);
        return dartsPersistence.save(transcription);
    }
}
