package uk.gov.hmcts.darts.transcriptions.validator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;

import java.util.Set;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.CLOSED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.COMPLETE;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REJECTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.OTHER;

@Component
@Slf4j
public class WorkflowValidator {

    public boolean isAutomatedTranscription(TranscriptionEntity transcription) {
        return OTHER.getId().equals(transcription.getTranscriptionType().getId());
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
            default -> handleInvalidTranscriptionWorkflow(transcription, expectedTranscriptionStatus);
        }
    }

    private void validateChangingToRequestedStatus(TranscriptionEntity transcription) {
        Set<TranscriptionStatusEnum> expectedPreviousStatuses = Set.of(REQUESTED);
        TranscriptionStatusEntity transcriptionStatusEntity = transcription.getTranscriptionStatus();
        if (!isNull(transcriptionStatusEntity) && isNotExpectedTranscriptionStatuses(transcription, expectedPreviousStatuses)) {
            handleInvalidTranscriptionWorkflow(transcription, REQUESTED);
        }
    }

    private static boolean isNotExpectedTranscriptionStatuses(TranscriptionEntity transcription, Set<TranscriptionStatusEnum> expectedPreviousStatuses) {
        return isNull(transcription.getTranscriptionStatus())
        || !expectedPreviousStatuses.contains(TranscriptionStatusEnum.fromId(transcription.getTranscriptionStatus().getId()));
    }

    private void validateChangingToAwaitingAuthorisationStatus(TranscriptionEntity transcription) {
        Set<TranscriptionStatusEnum> expectedPreviousStatuses = Set.of(REQUESTED);
        if (isAutomatedTranscription(transcription)
            || isNotExpectedTranscriptionStatuses(transcription, expectedPreviousStatuses)) {
            handleInvalidTranscriptionWorkflow(transcription, AWAITING_AUTHORISATION);
        }
    }

    private void validateChangingToApprovedStatus(TranscriptionEntity transcription) {
        Set<TranscriptionStatusEnum> expectedPreviousAutomatedStatuses = Set.of(REQUESTED);
        Set<TranscriptionStatusEnum> expectedPreviousManualStatuses = Set.of(AWAITING_AUTHORISATION);

        if (isAutomatedTranscription(transcription)
            && isNotExpectedTranscriptionStatuses(transcription, expectedPreviousAutomatedStatuses)) {
            handleInvalidTranscriptionWorkflow(transcription, APPROVED);
        } else if (!isAutomatedTranscription(transcription)
            && isNotExpectedTranscriptionStatuses(transcription, expectedPreviousManualStatuses)) {
            handleInvalidTranscriptionWorkflow(transcription, APPROVED);
        }
    }

    private void validateChangingToWithTranscriberStatus(TranscriptionEntity transcription) {
        Set<TranscriptionStatusEnum> expectedPreviousStatuses = Set.of(APPROVED);
        if (isNotExpectedTranscriptionStatuses(transcription, expectedPreviousStatuses)) {
            handleInvalidTranscriptionWorkflow(transcription, WITH_TRANSCRIBER);
        }
    }

    private void validateChangingToCompleteStatus(TranscriptionEntity transcription) {
        Set<TranscriptionStatusEnum> expectedPreviousStatuses = Set.of(WITH_TRANSCRIBER);
        if (isNotExpectedTranscriptionStatuses(transcription, expectedPreviousStatuses)) {
            handleInvalidTranscriptionWorkflow(transcription, COMPLETE);
        }
    }

    private void validateChangingToRejectedStatus(TranscriptionEntity transcription) {
        Set<TranscriptionStatusEnum> expectedPreviousStatuses = Set.of(AWAITING_AUTHORISATION);
        if (isAutomatedTranscription(transcription)
            || isNotExpectedTranscriptionStatuses(transcription, expectedPreviousStatuses)) {
            handleInvalidTranscriptionWorkflow(transcription, REJECTED);
        }
    }

    private void validateChangingToClosedStatus(TranscriptionEntity transcription) {
        Set<TranscriptionStatusEnum> expectedPreviousStatuses = Set.of(REQUESTED, AWAITING_AUTHORISATION, APPROVED, WITH_TRANSCRIBER);
        if (isNotExpectedTranscriptionStatuses(transcription, expectedPreviousStatuses)) {
            handleInvalidTranscriptionWorkflow(transcription, CLOSED);
        }
    }

    private void handleInvalidTranscriptionWorkflow(TranscriptionEntity transcription, TranscriptionStatusEnum expectedTranscriptionStatus) {
        TranscriptionStatusEnum transcriptionStatusEnum = null;
        TranscriptionStatusEntity transcriptionStatusEntity = transcription.getTranscriptionStatus();
        if (!isNull(transcriptionStatusEntity)) {
            transcriptionStatusEnum = TranscriptionStatusEnum.fromId(transcriptionStatusEntity.getId());
        }
        log.warn("Unable to go from workflow state {} to {}", transcriptionStatusEnum, expectedTranscriptionStatus);
        throw new DartsApiException(TranscriptionApiError.FAILED_TO_VALIDATE_TRANSCRIPTION_REQUEST);
    }

}
