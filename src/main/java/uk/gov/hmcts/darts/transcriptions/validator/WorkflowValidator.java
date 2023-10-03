package uk.gov.hmcts.darts.transcriptions.validator;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;

import java.util.Set;

import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.OTHER;

@Component
public class WorkflowValidator {

    public boolean isAutomatedTranscription(TranscriptionEntity transcription) {
        if (OTHER.getId().equals(transcription.getTranscriptionType().getId())) {
            return true;
        }
        return false;
    }

    public void validateChangeToWorkflowStatus(TranscriptionEntity transcription, TranscriptionStatusEnum expectedTranscriptionStatus) {

        switch (expectedTranscriptionStatus) {
            case REQUESTED -> validateChangingToRequestedStatus(transcription);
            case AWAITING_AUTHORISATION -> validateChangingToAwaitingAuthorisationStatus(transcription);
            case APPROVED -> validateChangingToApprovedStatus(transcription);
            case WITH_TRANSCRIBER -> validateChangingToWithTranscriberStatus(transcription);
            case COMPLETE -> validateChangingToCompleteStatus(transcription);
            case REJECTED -> validateChangingToRejectedStatus(transcription);
            case CLOSED -> validateChangingToClosedStatus(transcription);
            default -> handleInvalidTranscriptionWorkflow(transcription);
        }
    }

    private void validateChangingToRequestedStatus(TranscriptionEntity transcription) {
        handleInvalidTranscriptionWorkflow(transcription);
    }

    private static boolean isExpectedTranscriptionStatuses(TranscriptionEntity transcription,
                                                           Set<TranscriptionStatusEnum> expectedPreviousStatuses) {
        return expectedPreviousStatuses.contains(TranscriptionStatusEnum.fromId(transcription.getTranscriptionStatus().getId()));
    }

    private void validateChangingToAwaitingAuthorisationStatus(TranscriptionEntity transcription) {
        Set<TranscriptionStatusEnum> expectedPreviousStatuses = Set.of(REQUESTED);
        if (isAutomatedTranscription(transcription)
            || !isExpectedTranscriptionStatuses(transcription, expectedPreviousStatuses)) {
            handleInvalidTranscriptionWorkflow(transcription);
        }
    }

    private void validateChangingToApprovedStatus(TranscriptionEntity transcription) {
        Set<TranscriptionStatusEnum> expectedPreviousAutomatedStatuses = Set.of(REQUESTED);
        Set<TranscriptionStatusEnum> expectedPreviousManualStatuses = Set.of(AWAITING_AUTHORISATION);

        if (isAutomatedTranscription(transcription)
            && !isExpectedTranscriptionStatuses(transcription, expectedPreviousAutomatedStatuses)) {
            handleInvalidTranscriptionWorkflow(transcription);
        } else if (!isAutomatedTranscription(transcription)
            && !isExpectedTranscriptionStatuses(transcription, expectedPreviousManualStatuses)) {
            handleInvalidTranscriptionWorkflow(transcription);
        }
    }

    private void validateChangingToWithTranscriberStatus(TranscriptionEntity transcription) {
        Set<TranscriptionStatusEnum> expectedPreviousStatuses = Set.of(APPROVED);
        if (!isExpectedTranscriptionStatuses(transcription, expectedPreviousStatuses)) {
            handleInvalidTranscriptionWorkflow(transcription);
        }
    }

    private void validateChangingToCompleteStatus(TranscriptionEntity transcription) {
        Set<TranscriptionStatusEnum> expectedPreviousStatuses = Set.of(WITH_TRANSCRIBER);
        if (!isExpectedTranscriptionStatuses(transcription, expectedPreviousStatuses)) {
            handleInvalidTranscriptionWorkflow(transcription);
        }
    }

    private void validateChangingToRejectedStatus(TranscriptionEntity transcription) {
        Set<TranscriptionStatusEnum> expectedPreviousStatuses = Set.of(AWAITING_AUTHORISATION);
        if (isAutomatedTranscription(transcription)
            || !isExpectedTranscriptionStatuses(transcription, expectedPreviousStatuses)) {
            handleInvalidTranscriptionWorkflow(transcription);
        }
    }

    private void validateChangingToClosedStatus(TranscriptionEntity transcription) {
        Set<TranscriptionStatusEnum> expectedPreviousStatuses = Set.of(REQUESTED, AWAITING_AUTHORISATION, APPROVED, WITH_TRANSCRIBER);
        if (!isExpectedTranscriptionStatuses(transcription, expectedPreviousStatuses)) {
            handleInvalidTranscriptionWorkflow(transcription);
        }
    }

    private void handleInvalidTranscriptionWorkflow(TranscriptionEntity transcription) {
        if (!isAutomatedTranscription(transcription)) {
            throw new DartsApiException(TranscriptionApiError.FAILED_TO_VALIDATE_TRANSCRIPTION_REQUEST);
        } else {
            //TBD
            throw new IllegalArgumentException("Invalid transcription workflow");
        }
    }

}
