package uk.gov.hmcts.darts.transcriptions.validator;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.CLOSED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.COMPLETE;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REJECTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;

@ExtendWith(MockitoExtension.class)
class WorkflowValidatorTest {

    private final WorkflowValidator workflowValidator = new WorkflowValidator();

    @ParameterizedTest
    @Order(1)
    @EnumSource(names = {"AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateManualChangeToWorkflowStatusRequestedReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.COURT_LOG, currentTranscriptionStatus, REQUESTED)
        );
    }

    @ParameterizedTest
    @Order(2)
    @EnumSource(names = {"AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateAutomaticChangeToWorkflowStatusRequestedReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.OTHER, currentTranscriptionStatus, REQUESTED)
        );
    }

    @ParameterizedTest
    @Order(3)
    @EnumSource(names = {"SENTENCING_REMARKS", "INCLUDING_VERDICT", "ANTECEDENTS", "ARGUMENT_AND_SUBMISSION_OF_RULING", "COURT_LOG",
        "MITIGATION", "PROCEEDINGS_AFTER_VERDICT", "PROSECUTION_OPENING_OF_FACTS", "SPECIFIED_TIMES"})
    void validateManualChangeToWorkflowStatusAwaitingAuthorisationSuccess(TranscriptionTypeEnum transcriptionTypeEnum) {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(transcriptionTypeEnum, REQUESTED, AWAITING_AUTHORISATION));
    }

    @ParameterizedTest
    @Order(4)
    @EnumSource(names = {"AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateManualChangeToWorkflowStatusAwaitingAuthorisationReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {
        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.SENTENCING_REMARKS, currentTranscriptionStatus, AWAITING_AUTHORISATION)
        );
    }

    @ParameterizedTest
    @Order(5)
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateAutomaticChangeToWorkflowStatusAwaitingAuthorisationReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.OTHER, currentTranscriptionStatus, AWAITING_AUTHORISATION)
        );
    }

    @ParameterizedTest
    @Order(6)
    @EnumSource(names = {"SENTENCING_REMARKS", "INCLUDING_VERDICT", "ANTECEDENTS", "ARGUMENT_AND_SUBMISSION_OF_RULING", "COURT_LOG",
        "MITIGATION", "PROCEEDINGS_AFTER_VERDICT", "PROSECUTION_OPENING_OF_FACTS", "SPECIFIED_TIMES"})
    void validateManualChangeToApprovedWorkflowStatusSuccess(TranscriptionTypeEnum transcriptionTypeEnum) {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(transcriptionTypeEnum, AWAITING_AUTHORISATION, APPROVED));
    }

    @Test
    @Order(7)
    void validateAutomaticChangeToApprovedWorkflowStatusSuccess() {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.OTHER, REQUESTED, APPROVED));
    }

    @ParameterizedTest
    @Order(8)
    @EnumSource(names = {"REQUESTED", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateManualChangeToApprovedWorkflowStatusReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.INCLUDING_VERDICT, currentTranscriptionStatus, APPROVED)
        );
    }

    @ParameterizedTest
    @Order(9)
    @EnumSource(names = {"AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateAutomaticChangeToApprovedWorkflowStatusReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.OTHER, currentTranscriptionStatus, APPROVED)
        );
    }

    @ParameterizedTest
    @Order(10)
    @EnumSource(names = {"SENTENCING_REMARKS", "INCLUDING_VERDICT", "ANTECEDENTS", "ARGUMENT_AND_SUBMISSION_OF_RULING", "COURT_LOG",
        "MITIGATION", "PROCEEDINGS_AFTER_VERDICT", "PROSECUTION_OPENING_OF_FACTS", "SPECIFIED_TIMES"})
    void validateManualChangeToWorkflowStatusRejectedSuccess(TranscriptionTypeEnum transcriptionTypeEnum) {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(transcriptionTypeEnum, AWAITING_AUTHORISATION, REJECTED));
    }

    @ParameterizedTest
    @Order(11)
    @EnumSource(names = {"REQUESTED", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateManualChangeToWorkflowStatusRejectedReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.SENTENCING_REMARKS, currentTranscriptionStatus, REJECTED)
        );
    }

    @ParameterizedTest
    @Order(12)
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateAutomaticChangeToWorkflowStatusRejectedReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.OTHER, currentTranscriptionStatus, REJECTED)
        );
    }

    @ParameterizedTest
    @Order(13)
    @EnumSource(names = {"SENTENCING_REMARKS", "INCLUDING_VERDICT", "ANTECEDENTS", "ARGUMENT_AND_SUBMISSION_OF_RULING", "COURT_LOG",
        "MITIGATION", "PROCEEDINGS_AFTER_VERDICT", "PROSECUTION_OPENING_OF_FACTS", "SPECIFIED_TIMES"})
    void validateManualChangeToWorkflowStatusWithTranscriberSuccess(TranscriptionTypeEnum transcriptionTypeEnum) {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(transcriptionTypeEnum, APPROVED, WITH_TRANSCRIBER));
    }

    @Test
    @Order(14)
    void validateAutomaticChangeToWorkflowStatusWithTranscriberSuccess() {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.OTHER, APPROVED, WITH_TRANSCRIBER));
    }

    @ParameterizedTest
    @Order(15)
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateManualChangeToWorkflowStatusWithTranscriberReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.COURT_LOG, currentTranscriptionStatus, WITH_TRANSCRIBER)
        );
    }

    @ParameterizedTest
    @Order(16)
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateAutomaticChangeToWorkflowStatusWithTranscriberReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.OTHER, currentTranscriptionStatus, WITH_TRANSCRIBER)
        );
    }

    @ParameterizedTest
    @Order(17)
    @EnumSource(names = {"SENTENCING_REMARKS", "INCLUDING_VERDICT", "ANTECEDENTS", "ARGUMENT_AND_SUBMISSION_OF_RULING", "COURT_LOG",
        "MITIGATION", "PROCEEDINGS_AFTER_VERDICT", "PROSECUTION_OPENING_OF_FACTS", "SPECIFIED_TIMES"})
    void validateManualChangeToWorkflowStatusCompleteSuccess(TranscriptionTypeEnum transcriptionTypeEnum) {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(transcriptionTypeEnum, WITH_TRANSCRIBER, COMPLETE));
    }

    @Test
    @Order(18)
    void validateAutomaticChangeToWorkflowStatusCompleteSuccess() {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.OTHER, WITH_TRANSCRIBER, COMPLETE));
    }

    @ParameterizedTest
    @Order(19)
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "COMPLETE", "CLOSED"})
    void validateManualChangeToWorkflowStatusCompleteReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.COURT_LOG, currentTranscriptionStatus, COMPLETE)
        );
    }

    @ParameterizedTest
    @Order(20)
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "COMPLETE", "CLOSED"})
    void validateAutomaticChangeToWorkflowStatusCompleteReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.OTHER, currentTranscriptionStatus, COMPLETE)
        );
    }

    @ParameterizedTest
    @Order(21)
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "APPROVED", "WITH_TRANSCRIBER"})
    void validateManualChangeToWorkflowStatusClosedSuccess(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.MITIGATION, currentTranscriptionStatus, CLOSED));
    }

    @ParameterizedTest
    @Order(21)
    @EnumSource(names = {"REQUESTED", "APPROVED", "WITH_TRANSCRIBER"})
    void validateAutomaticChangeToWorkflowStatusClosedSuccess(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.OTHER, currentTranscriptionStatus, CLOSED));
    }

    @ParameterizedTest
    @Order(22)
    @EnumSource(names = {"REJECTED", "CLOSED"})
    void validateManualChangeToWorkflowStatusClosedReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.COURT_LOG, currentTranscriptionStatus, CLOSED)
        );
    }

    @ParameterizedTest
    @Order(23)
    @EnumSource(names = {"REJECTED", "CLOSED"})
    void validateAutomaticChangeToWorkflowStatusClosedReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.OTHER, currentTranscriptionStatus, CLOSED)
        );
    }

}
