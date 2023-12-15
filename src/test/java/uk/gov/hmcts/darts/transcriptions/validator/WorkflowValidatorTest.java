package uk.gov.hmcts.darts.transcriptions.validator;

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
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.COURT_LOG;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.INCLUDING_VERDICT;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.MITIGATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.OTHER;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.SENTENCING_REMARKS;

@ExtendWith(MockitoExtension.class)
class WorkflowValidatorTest {

    private final WorkflowValidator workflowValidator = new WorkflowValidator();

    @ParameterizedTest
    @EnumSource(names = {"AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateManualChangeToWorkflowStatusRequestedReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(true, COURT_LOG, currentTranscriptionStatus, REQUESTED)
        );
    }

    @ParameterizedTest
    @EnumSource(names = {"AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateAutomaticChangeToWorkflowStatusRequestedReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(false, OTHER, currentTranscriptionStatus, REQUESTED)
        );
    }

    @ParameterizedTest
    @EnumSource(names = {"SENTENCING_REMARKS", "INCLUDING_VERDICT", "ANTECEDENTS", "ARGUMENT_AND_SUBMISSION_OF_RULING", "COURT_LOG",
        "MITIGATION", "PROCEEDINGS_AFTER_VERDICT", "PROSECUTION_OPENING_OF_FACTS", "SPECIFIED_TIMES"})
    void validateManualChangeToWorkflowStatusAwaitingAuthorisationSuccess(TranscriptionTypeEnum transcriptionTypeEnum) {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(true, transcriptionTypeEnum, REQUESTED, AWAITING_AUTHORISATION));
    }

    @ParameterizedTest
    @EnumSource(names = {"AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateManualChangeToWorkflowStatusAwaitingAuthorisationReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {
        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(true, SENTENCING_REMARKS, currentTranscriptionStatus, AWAITING_AUTHORISATION)
        );
    }

    @ParameterizedTest
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateAutomaticChangeToWorkflowStatusAwaitingAuthorisationReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(false, OTHER, currentTranscriptionStatus, AWAITING_AUTHORISATION)
        );
    }

    @ParameterizedTest
    @EnumSource(names = {"SENTENCING_REMARKS", "INCLUDING_VERDICT", "ANTECEDENTS", "ARGUMENT_AND_SUBMISSION_OF_RULING", "COURT_LOG",
        "MITIGATION", "PROCEEDINGS_AFTER_VERDICT", "PROSECUTION_OPENING_OF_FACTS", "SPECIFIED_TIMES"})
    void validateManualChangeToApprovedWorkflowStatusSuccess(TranscriptionTypeEnum transcriptionTypeEnum) {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(true, transcriptionTypeEnum, AWAITING_AUTHORISATION, APPROVED));
    }

    @Test
    void validateAutomaticChangeToApprovedWorkflowStatusSuccess() {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(false, OTHER, REQUESTED, APPROVED));
    }

    @ParameterizedTest
    @EnumSource(names = {"REQUESTED", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateManualChangeToApprovedWorkflowStatusReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(true, INCLUDING_VERDICT, currentTranscriptionStatus, APPROVED)
        );
    }

    @ParameterizedTest
    @EnumSource(names = {"AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateAutomaticChangeToApprovedWorkflowStatusReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(false, OTHER, currentTranscriptionStatus, APPROVED)
        );
    }

    @ParameterizedTest
    @EnumSource(names = {"SENTENCING_REMARKS", "INCLUDING_VERDICT", "ANTECEDENTS", "ARGUMENT_AND_SUBMISSION_OF_RULING", "COURT_LOG",
        "MITIGATION", "PROCEEDINGS_AFTER_VERDICT", "PROSECUTION_OPENING_OF_FACTS", "SPECIFIED_TIMES"})
    void validateManualChangeToWorkflowStatusRejectedSuccess(TranscriptionTypeEnum transcriptionTypeEnum) {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(true, transcriptionTypeEnum, AWAITING_AUTHORISATION, REJECTED));
    }

    @ParameterizedTest
    @EnumSource(names = {"REQUESTED", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateManualChangeToWorkflowStatusRejectedReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(true, SENTENCING_REMARKS, currentTranscriptionStatus, REJECTED)
        );
    }

    @ParameterizedTest
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateAutomaticChangeToWorkflowStatusRejectedReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(false, OTHER, currentTranscriptionStatus, REJECTED)
        );
    }

    @ParameterizedTest
    @EnumSource(names = {"SENTENCING_REMARKS", "INCLUDING_VERDICT", "ANTECEDENTS", "ARGUMENT_AND_SUBMISSION_OF_RULING", "COURT_LOG",
        "MITIGATION", "PROCEEDINGS_AFTER_VERDICT", "PROSECUTION_OPENING_OF_FACTS", "SPECIFIED_TIMES"})
    void validateManualChangeToWorkflowStatusWithTranscriberSuccess(TranscriptionTypeEnum transcriptionTypeEnum) {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(true, transcriptionTypeEnum, APPROVED, WITH_TRANSCRIBER));
    }

    @Test

    void validateAutomaticChangeToWorkflowStatusWithTranscriberSuccess() {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(false, OTHER, APPROVED, WITH_TRANSCRIBER));
    }

    @ParameterizedTest
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateManualChangeToWorkflowStatusWithTranscriberReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(true, COURT_LOG, currentTranscriptionStatus, WITH_TRANSCRIBER)
        );
    }

    @ParameterizedTest
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateAutomaticChangeToWorkflowStatusWithTranscriberReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(false, OTHER, currentTranscriptionStatus, WITH_TRANSCRIBER)
        );
    }

    @ParameterizedTest
    @EnumSource(names = {"SENTENCING_REMARKS", "INCLUDING_VERDICT", "ANTECEDENTS", "ARGUMENT_AND_SUBMISSION_OF_RULING", "COURT_LOG",
        "MITIGATION", "PROCEEDINGS_AFTER_VERDICT", "PROSECUTION_OPENING_OF_FACTS", "SPECIFIED_TIMES"})
    void validateManualChangeToWorkflowStatusCompleteSuccess(TranscriptionTypeEnum transcriptionTypeEnum) {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(true, transcriptionTypeEnum, WITH_TRANSCRIBER, COMPLETE));
    }

    @Test

    void validateAutomaticChangeToWorkflowStatusCompleteSuccess() {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(false, OTHER, WITH_TRANSCRIBER, COMPLETE));
    }

    @ParameterizedTest
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "COMPLETE", "CLOSED"})
    void validateManualChangeToWorkflowStatusCompleteReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(true, COURT_LOG, currentTranscriptionStatus, COMPLETE)
        );
    }

    @ParameterizedTest
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "COMPLETE", "CLOSED"})
    void validateAutomaticChangeToWorkflowStatusCompleteReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(false, OTHER, currentTranscriptionStatus, COMPLETE)
        );
    }

    @ParameterizedTest
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "APPROVED", "WITH_TRANSCRIBER"})
    void validateManualChangeToWorkflowStatusClosedSuccess(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(true, MITIGATION, currentTranscriptionStatus, CLOSED));
    }

    @ParameterizedTest
    @EnumSource(names = {"REQUESTED", "APPROVED", "WITH_TRANSCRIBER"})
    void validateAutomaticChangeToWorkflowStatusClosedSuccess(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(false, OTHER, currentTranscriptionStatus, CLOSED));
    }

    @ParameterizedTest
    @EnumSource(names = {"REJECTED", "CLOSED"})
    void validateManualChangeToWorkflowStatusClosedReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(true, COURT_LOG, currentTranscriptionStatus, CLOSED)
        );
    }

    @ParameterizedTest
    @EnumSource(names = {"REJECTED", "CLOSED"})
    void validateAutomaticChangeToWorkflowStatusClosedReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(false, OTHER, currentTranscriptionStatus, CLOSED)
        );
    }

}
