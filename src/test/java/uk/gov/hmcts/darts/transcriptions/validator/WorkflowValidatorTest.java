package uk.gov.hmcts.darts.transcriptions.validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.validator.model.StatusTransitionCheck;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.CLOSED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.COMPLETE;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REJECTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.UNFULFILLED;
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
            workflowValidator.validateChangeToWorkflowStatus(true, COURT_LOG, currentTranscriptionStatus, REQUESTED, false)
        );
    }

    @Test
    void validateManualChangeAdmin() {
        List<StatusTransitionCheck> listOfChecks = new ArrayList<>();
        listOfChecks.add(new StatusTransitionCheck(REQUESTED, REQUESTED, false));
        listOfChecks.add(new StatusTransitionCheck(REQUESTED, AWAITING_AUTHORISATION, false));
        listOfChecks.add(new StatusTransitionCheck(REQUESTED, APPROVED, false));
        listOfChecks.add(new StatusTransitionCheck(REQUESTED, REJECTED, false));
        listOfChecks.add(new StatusTransitionCheck(REQUESTED, WITH_TRANSCRIBER, false));
        listOfChecks.add(new StatusTransitionCheck(REQUESTED, COMPLETE, false));
        listOfChecks.add(new StatusTransitionCheck(REQUESTED, CLOSED, true));
        listOfChecks.add(new StatusTransitionCheck(REQUESTED, UNFULFILLED, false));

        listOfChecks.add(new StatusTransitionCheck(AWAITING_AUTHORISATION, REQUESTED, true));
        listOfChecks.add(new StatusTransitionCheck(AWAITING_AUTHORISATION, AWAITING_AUTHORISATION, false));
        listOfChecks.add(new StatusTransitionCheck(AWAITING_AUTHORISATION, APPROVED, false));
        listOfChecks.add(new StatusTransitionCheck(AWAITING_AUTHORISATION, REJECTED, false));
        listOfChecks.add(new StatusTransitionCheck(AWAITING_AUTHORISATION, WITH_TRANSCRIBER, false));
        listOfChecks.add(new StatusTransitionCheck(AWAITING_AUTHORISATION, COMPLETE, false));
        listOfChecks.add(new StatusTransitionCheck(AWAITING_AUTHORISATION, CLOSED, true));
        listOfChecks.add(new StatusTransitionCheck(AWAITING_AUTHORISATION, UNFULFILLED, false));

        listOfChecks.add(new StatusTransitionCheck(APPROVED, REQUESTED, false));
        listOfChecks.add(new StatusTransitionCheck(APPROVED, AWAITING_AUTHORISATION, false));
        listOfChecks.add(new StatusTransitionCheck(APPROVED, APPROVED, false));
        listOfChecks.add(new StatusTransitionCheck(APPROVED, REJECTED, false));
        listOfChecks.add(new StatusTransitionCheck(APPROVED, WITH_TRANSCRIBER, false));
        listOfChecks.add(new StatusTransitionCheck(APPROVED, COMPLETE, false));
        listOfChecks.add(new StatusTransitionCheck(APPROVED, CLOSED, true));
        listOfChecks.add(new StatusTransitionCheck(APPROVED, UNFULFILLED, false));

        listOfChecks.add(new StatusTransitionCheck(REJECTED, REQUESTED, false));
        listOfChecks.add(new StatusTransitionCheck(REJECTED, AWAITING_AUTHORISATION, false));
        listOfChecks.add(new StatusTransitionCheck(REJECTED, APPROVED, false));
        listOfChecks.add(new StatusTransitionCheck(REJECTED, REJECTED, false));
        listOfChecks.add(new StatusTransitionCheck(REJECTED, WITH_TRANSCRIBER, false));
        listOfChecks.add(new StatusTransitionCheck(REJECTED, COMPLETE, false));
        listOfChecks.add(new StatusTransitionCheck(REJECTED, CLOSED, false));
        listOfChecks.add(new StatusTransitionCheck(REJECTED, UNFULFILLED, false));

        listOfChecks.add(new StatusTransitionCheck(WITH_TRANSCRIBER, REQUESTED, false));
        listOfChecks.add(new StatusTransitionCheck(WITH_TRANSCRIBER, AWAITING_AUTHORISATION, false));
        listOfChecks.add(new StatusTransitionCheck(WITH_TRANSCRIBER, APPROVED, true));
        listOfChecks.add(new StatusTransitionCheck(WITH_TRANSCRIBER, REJECTED, false));
        listOfChecks.add(new StatusTransitionCheck(WITH_TRANSCRIBER, WITH_TRANSCRIBER, false));
        listOfChecks.add(new StatusTransitionCheck(WITH_TRANSCRIBER, COMPLETE, false));
        listOfChecks.add(new StatusTransitionCheck(WITH_TRANSCRIBER, CLOSED, true));
        listOfChecks.add(new StatusTransitionCheck(WITH_TRANSCRIBER, UNFULFILLED, true));

        listOfChecks.add(new StatusTransitionCheck(COMPLETE, REQUESTED, false));
        listOfChecks.add(new StatusTransitionCheck(COMPLETE, AWAITING_AUTHORISATION, false));
        listOfChecks.add(new StatusTransitionCheck(COMPLETE, APPROVED, false));
        listOfChecks.add(new StatusTransitionCheck(COMPLETE, REJECTED, false));
        listOfChecks.add(new StatusTransitionCheck(COMPLETE, WITH_TRANSCRIBER, false));
        listOfChecks.add(new StatusTransitionCheck(COMPLETE, COMPLETE, false));
        listOfChecks.add(new StatusTransitionCheck(COMPLETE, CLOSED, false));
        listOfChecks.add(new StatusTransitionCheck(COMPLETE, UNFULFILLED, false));

        listOfChecks.add(new StatusTransitionCheck(CLOSED, REQUESTED, false));
        listOfChecks.add(new StatusTransitionCheck(CLOSED, AWAITING_AUTHORISATION, false));
        listOfChecks.add(new StatusTransitionCheck(CLOSED, APPROVED, false));
        listOfChecks.add(new StatusTransitionCheck(CLOSED, REJECTED, false));
        listOfChecks.add(new StatusTransitionCheck(CLOSED, WITH_TRANSCRIBER, false));
        listOfChecks.add(new StatusTransitionCheck(CLOSED, COMPLETE, false));
        listOfChecks.add(new StatusTransitionCheck(CLOSED, CLOSED, false));
        listOfChecks.add(new StatusTransitionCheck(CLOSED, UNFULFILLED, false));

        listOfChecks.add(new StatusTransitionCheck(UNFULFILLED, REQUESTED, false));
        listOfChecks.add(new StatusTransitionCheck(UNFULFILLED, AWAITING_AUTHORISATION, false));
        listOfChecks.add(new StatusTransitionCheck(UNFULFILLED, APPROVED, false));
        listOfChecks.add(new StatusTransitionCheck(UNFULFILLED, REJECTED, false));
        listOfChecks.add(new StatusTransitionCheck(UNFULFILLED, WITH_TRANSCRIBER, false));
        listOfChecks.add(new StatusTransitionCheck(UNFULFILLED, COMPLETE, false));
        listOfChecks.add(new StatusTransitionCheck(UNFULFILLED, CLOSED, false));
        listOfChecks.add(new StatusTransitionCheck(UNFULFILLED, UNFULFILLED, false));

        for (StatusTransitionCheck check : listOfChecks) {
            assertEquals(check.isShouldBeAllowed(),
                         workflowValidator.validateChangeToWorkflowStatus(true, COURT_LOG, check.getFromStatus(), check.getToStatus(), true),
                         "should have been " + check.isShouldBeAllowed() + " for moving from " + check.getFromStatus() + " to " + check.getToStatus());
        }
    }

    @Test
    void validateAutoChangeAdmin() {
        List<StatusTransitionCheck> listOfChecks = new ArrayList<>();
        listOfChecks.add(new StatusTransitionCheck(REQUESTED, REQUESTED, false));
        listOfChecks.add(new StatusTransitionCheck(REQUESTED, AWAITING_AUTHORISATION, false));
        listOfChecks.add(new StatusTransitionCheck(REQUESTED, APPROVED, false));
        listOfChecks.add(new StatusTransitionCheck(REQUESTED, REJECTED, false));
        listOfChecks.add(new StatusTransitionCheck(REQUESTED, WITH_TRANSCRIBER, false));
        listOfChecks.add(new StatusTransitionCheck(REQUESTED, COMPLETE, false));
        listOfChecks.add(new StatusTransitionCheck(REQUESTED, CLOSED, true));
        listOfChecks.add(new StatusTransitionCheck(REQUESTED, UNFULFILLED, false));

        listOfChecks.add(new StatusTransitionCheck(APPROVED, REQUESTED, false));
        listOfChecks.add(new StatusTransitionCheck(APPROVED, AWAITING_AUTHORISATION, false));
        listOfChecks.add(new StatusTransitionCheck(APPROVED, APPROVED, false));
        listOfChecks.add(new StatusTransitionCheck(APPROVED, REJECTED, false));
        listOfChecks.add(new StatusTransitionCheck(APPROVED, WITH_TRANSCRIBER, false));
        listOfChecks.add(new StatusTransitionCheck(APPROVED, COMPLETE, false));
        listOfChecks.add(new StatusTransitionCheck(APPROVED, CLOSED, true));
        listOfChecks.add(new StatusTransitionCheck(APPROVED, UNFULFILLED, false));

        listOfChecks.add(new StatusTransitionCheck(WITH_TRANSCRIBER, REQUESTED, false));
        listOfChecks.add(new StatusTransitionCheck(WITH_TRANSCRIBER, AWAITING_AUTHORISATION, false));
        listOfChecks.add(new StatusTransitionCheck(WITH_TRANSCRIBER, APPROVED, true));
        listOfChecks.add(new StatusTransitionCheck(WITH_TRANSCRIBER, REJECTED, false));
        listOfChecks.add(new StatusTransitionCheck(WITH_TRANSCRIBER, WITH_TRANSCRIBER, false));
        listOfChecks.add(new StatusTransitionCheck(WITH_TRANSCRIBER, COMPLETE, true));
        listOfChecks.add(new StatusTransitionCheck(WITH_TRANSCRIBER, CLOSED, true));
        listOfChecks.add(new StatusTransitionCheck(WITH_TRANSCRIBER, UNFULFILLED, true));

        listOfChecks.add(new StatusTransitionCheck(COMPLETE, REQUESTED, false));
        listOfChecks.add(new StatusTransitionCheck(COMPLETE, AWAITING_AUTHORISATION, false));
        listOfChecks.add(new StatusTransitionCheck(COMPLETE, APPROVED, false));
        listOfChecks.add(new StatusTransitionCheck(COMPLETE, REJECTED, false));
        listOfChecks.add(new StatusTransitionCheck(COMPLETE, WITH_TRANSCRIBER, false));
        listOfChecks.add(new StatusTransitionCheck(COMPLETE, COMPLETE, false));
        listOfChecks.add(new StatusTransitionCheck(COMPLETE, CLOSED, false));
        listOfChecks.add(new StatusTransitionCheck(COMPLETE, UNFULFILLED, false));

        listOfChecks.add(new StatusTransitionCheck(CLOSED, REQUESTED, false));
        listOfChecks.add(new StatusTransitionCheck(CLOSED, AWAITING_AUTHORISATION, false));
        listOfChecks.add(new StatusTransitionCheck(CLOSED, APPROVED, false));
        listOfChecks.add(new StatusTransitionCheck(CLOSED, REJECTED, false));
        listOfChecks.add(new StatusTransitionCheck(CLOSED, WITH_TRANSCRIBER, false));
        listOfChecks.add(new StatusTransitionCheck(CLOSED, COMPLETE, false));
        listOfChecks.add(new StatusTransitionCheck(CLOSED, CLOSED, false));
        listOfChecks.add(new StatusTransitionCheck(CLOSED, UNFULFILLED, false));

        listOfChecks.add(new StatusTransitionCheck(UNFULFILLED, REQUESTED, false));
        listOfChecks.add(new StatusTransitionCheck(UNFULFILLED, AWAITING_AUTHORISATION, false));
        listOfChecks.add(new StatusTransitionCheck(UNFULFILLED, APPROVED, false));
        listOfChecks.add(new StatusTransitionCheck(UNFULFILLED, REJECTED, false));
        listOfChecks.add(new StatusTransitionCheck(UNFULFILLED, WITH_TRANSCRIBER, false));
        listOfChecks.add(new StatusTransitionCheck(UNFULFILLED, COMPLETE, false));
        listOfChecks.add(new StatusTransitionCheck(UNFULFILLED, CLOSED, false));
        listOfChecks.add(new StatusTransitionCheck(UNFULFILLED, UNFULFILLED, false));

        for (StatusTransitionCheck check : listOfChecks) {
            assertEquals(check.isShouldBeAllowed(),
                         workflowValidator.validateChangeToWorkflowStatus(false, COURT_LOG, check.getFromStatus(), check.getToStatus(), true),
                         "should have been " + check.isShouldBeAllowed() + " for moving from " + check.getFromStatus() + " to " + check.getToStatus());
        }
    }

    @ParameterizedTest
    @EnumSource(names = {"AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED", "UNFULFILLED"})
    void validateAutomaticChangeToWorkflowStatusRequestedReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(false, OTHER, currentTranscriptionStatus, REQUESTED, false)
        );
    }

    @ParameterizedTest
    @EnumSource(names = {"SENTENCING_REMARKS", "INCLUDING_VERDICT", "ANTECEDENTS", "ARGUMENT_AND_SUBMISSION_OF_RULING", "COURT_LOG",
        "MITIGATION", "PROCEEDINGS_AFTER_VERDICT", "PROSECUTION_OPENING_OF_FACTS", "SPECIFIED_TIMES"})
    void validateManualChangeToWorkflowStatusAwaitingAuthorisationSuccess(TranscriptionTypeEnum transcriptionTypeEnum) {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(true, transcriptionTypeEnum, REQUESTED, AWAITING_AUTHORISATION, false));
    }

    @ParameterizedTest
    @EnumSource(names = {"AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED", "UNFULFILLED"})
    void validateManualChangeToWorkflowStatusAwaitingAuthorisationReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {
        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(true, SENTENCING_REMARKS, currentTranscriptionStatus, AWAITING_AUTHORISATION, false)
        );
    }

    @ParameterizedTest
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED", "UNFULFILLED"})
    void validateAutomaticChangeToWorkflowStatusAwaitingAuthorisationReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(false, OTHER, currentTranscriptionStatus, AWAITING_AUTHORISATION, false)
        );
    }

    @ParameterizedTest
    @EnumSource(names = {"SENTENCING_REMARKS", "INCLUDING_VERDICT", "ANTECEDENTS", "ARGUMENT_AND_SUBMISSION_OF_RULING", "COURT_LOG",
        "MITIGATION", "PROCEEDINGS_AFTER_VERDICT", "PROSECUTION_OPENING_OF_FACTS", "SPECIFIED_TIMES"})
    void validateManualChangeToApprovedWorkflowStatusSuccess(TranscriptionTypeEnum transcriptionTypeEnum) {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(true, transcriptionTypeEnum, AWAITING_AUTHORISATION, APPROVED, false));
    }

    @Test
    void validateAutomaticChangeToApprovedWorkflowStatusSuccess() {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(false, OTHER, REQUESTED, APPROVED, false));
    }

    @ParameterizedTest
    @EnumSource(names = {"REQUESTED", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED", "UNFULFILLED"})
    void validateManualChangeToApprovedWorkflowStatusReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(true, INCLUDING_VERDICT, currentTranscriptionStatus, APPROVED, false)
        );
    }

    @ParameterizedTest
    @EnumSource(names = {"AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED", "UNFULFILLED"})
    void validateAutomaticChangeToApprovedWorkflowStatusReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(false, OTHER, currentTranscriptionStatus, APPROVED, false)
        );
    }

    @ParameterizedTest
    @EnumSource(names = {"SENTENCING_REMARKS", "INCLUDING_VERDICT", "ANTECEDENTS", "ARGUMENT_AND_SUBMISSION_OF_RULING", "COURT_LOG",
        "MITIGATION", "PROCEEDINGS_AFTER_VERDICT", "PROSECUTION_OPENING_OF_FACTS", "SPECIFIED_TIMES"})
    void validateManualChangeToWorkflowStatusRejectedSuccess(TranscriptionTypeEnum transcriptionTypeEnum) {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(true, transcriptionTypeEnum, AWAITING_AUTHORISATION, REJECTED, false));
    }

    @ParameterizedTest
    @EnumSource(names = {"REQUESTED", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED", "UNFULFILLED"})
    void validateManualChangeToWorkflowStatusRejectedReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(true, SENTENCING_REMARKS, currentTranscriptionStatus, REJECTED, false)
        );
    }

    @ParameterizedTest
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED", "UNFULFILLED"})
    void validateAutomaticChangeToWorkflowStatusRejectedReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(false, OTHER, currentTranscriptionStatus, REJECTED, false)
        );
    }

    @ParameterizedTest
    @EnumSource(names = {"SENTENCING_REMARKS", "INCLUDING_VERDICT", "ANTECEDENTS", "ARGUMENT_AND_SUBMISSION_OF_RULING", "COURT_LOG",
        "MITIGATION", "PROCEEDINGS_AFTER_VERDICT", "PROSECUTION_OPENING_OF_FACTS", "SPECIFIED_TIMES"})
    void validateManualChangeToWorkflowStatusWithTranscriberSuccess(TranscriptionTypeEnum transcriptionTypeEnum) {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(true, transcriptionTypeEnum, APPROVED, WITH_TRANSCRIBER, false));
    }

    @Test
    void validateAutomaticChangeToWorkflowStatusWithTranscriberSuccess() {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(false, OTHER, APPROVED, WITH_TRANSCRIBER, false));
    }

    @ParameterizedTest
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateManualChangeToWorkflowStatusWithTranscriberReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(true, COURT_LOG, currentTranscriptionStatus, WITH_TRANSCRIBER, false)
        );
    }

    @ParameterizedTest
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateAutomaticChangeToWorkflowStatusWithTranscriberReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(false, OTHER, currentTranscriptionStatus, WITH_TRANSCRIBER, false)
        );
    }

    @ParameterizedTest
    @EnumSource(names = {"SENTENCING_REMARKS", "INCLUDING_VERDICT", "ANTECEDENTS", "ARGUMENT_AND_SUBMISSION_OF_RULING", "COURT_LOG",
        "MITIGATION", "PROCEEDINGS_AFTER_VERDICT", "PROSECUTION_OPENING_OF_FACTS", "SPECIFIED_TIMES"})
    void validateManualChangeToWorkflowStatusCompleteSuccess(TranscriptionTypeEnum transcriptionTypeEnum) {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(true, transcriptionTypeEnum, WITH_TRANSCRIBER, COMPLETE, false));
    }

    @Test
    void validateAutomaticChangeToWorkflowStatusCompleteSuccess() {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(false, OTHER, WITH_TRANSCRIBER, COMPLETE, false));
    }

    @ParameterizedTest
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "COMPLETE", "CLOSED", "UNFULFILLED"})
    void validateManualChangeToWorkflowStatusCompleteReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(true, COURT_LOG, currentTranscriptionStatus, COMPLETE, false)
        );
    }

    @ParameterizedTest
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "COMPLETE", "CLOSED", "UNFULFILLED"})
    void validateAutomaticChangeToWorkflowStatusCompleteReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(false, OTHER, currentTranscriptionStatus, COMPLETE, false)
        );
    }

    @ParameterizedTest
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "APPROVED", "WITH_TRANSCRIBER"})
    void validateManualChangeToWorkflowStatusClosedSuccess(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(true, MITIGATION, currentTranscriptionStatus, CLOSED, false));
    }

    @ParameterizedTest
    @EnumSource(names = {"REQUESTED", "APPROVED", "WITH_TRANSCRIBER"})
    void validateAutomaticChangeToWorkflowStatusClosedSuccess(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(false, OTHER, currentTranscriptionStatus, CLOSED, false));
    }

    @ParameterizedTest
    @EnumSource(names = {"REJECTED", "CLOSED", "UNFULFILLED", "COMPLETE"})
    void validateManualChangeToWorkflowStatusClosedReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(true, COURT_LOG, currentTranscriptionStatus, CLOSED, false)
        );
    }

    @ParameterizedTest
    @EnumSource(names = {"REJECTED", "CLOSED", "UNFULFILLED", "COMPLETE"})
    void validateAutomaticChangeToWorkflowStatusClosedReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(false, OTHER, currentTranscriptionStatus, CLOSED, false)
        );
    }


    @ParameterizedTest
    @EnumSource(names = {"WITH_TRANSCRIBER"})
    void validateManualChangeToWorkflowStatusUnfulfilledSuccess(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(true, MITIGATION, currentTranscriptionStatus, UNFULFILLED, false));
    }

    @ParameterizedTest
    @EnumSource(names = {"WITH_TRANSCRIBER"})
    void validateAutomaticChangeToWorkflowStatusUnfulfilledSuccess(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(false, OTHER, currentTranscriptionStatus, UNFULFILLED, false));
    }

    @ParameterizedTest
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "COMPLETE", "CLOSED", "UNFULFILLED"})
    void validateManualChangeToWorkflowStatusUnfulfilledReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(true, COURT_LOG, currentTranscriptionStatus, UNFULFILLED, false)
        );
    }

    @ParameterizedTest
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "COMPLETE", "CLOSED", "UNFULFILLED"})
    void validateAutomaticChangeToWorkflowStatusUnfulfilledReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(false, OTHER, currentTranscriptionStatus, UNFULFILLED, false)
        );
    }
}
