package uk.gov.hmcts.darts.transcriptions.validator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    // Creates a map defining the transition rules, from any given current state to a list of allowable target states
    final Map<TranscriptionStatusEnum, Set<TranscriptionStatusEnum>> manualWorkflowTransitionRules = new HashMap<>();

    final Map<TranscriptionStatusEnum, Set<TranscriptionStatusEnum>> automaticWorkflowTransitionRules = new HashMap<>();

    public WorkflowValidator() {
        manualWorkflowTransitionRules.put(REQUESTED, Set.of(REQUESTED));
        manualWorkflowTransitionRules.put(AWAITING_AUTHORISATION, Set.of(REQUESTED));
        manualWorkflowTransitionRules.put(APPROVED, Set.of(AWAITING_AUTHORISATION));
        manualWorkflowTransitionRules.put(REJECTED, Set.of(AWAITING_AUTHORISATION));
        manualWorkflowTransitionRules.put(WITH_TRANSCRIBER, Set.of(APPROVED));
        manualWorkflowTransitionRules.put(COMPLETE, Set.of(WITH_TRANSCRIBER));
        manualWorkflowTransitionRules.put(CLOSED, Set.of(REQUESTED, AWAITING_AUTHORISATION, APPROVED, WITH_TRANSCRIBER));

        automaticWorkflowTransitionRules.put(REQUESTED, Set.of(REQUESTED));
        automaticWorkflowTransitionRules.put(APPROVED, Set.of(REQUESTED));
        automaticWorkflowTransitionRules.put(WITH_TRANSCRIBER, Set.of(APPROVED));
        automaticWorkflowTransitionRules.put(COMPLETE, Set.of(WITH_TRANSCRIBER));
        automaticWorkflowTransitionRules.put(CLOSED, Set.of(REQUESTED, APPROVED, WITH_TRANSCRIBER));
    }

    public boolean isAutomatedTranscription(TranscriptionTypeEnum transcriptionTypeEnum) {
        return OTHER.equals(transcriptionTypeEnum);
    }

    public void validateChangeToWorkflowStatus(TranscriptionTypeEnum transcriptionTypeEnum,
                                               TranscriptionStatusEnum currentTranscriptionStatus,
                                               TranscriptionStatusEnum desiredTargetTranscriptionStatus) {
        if (isAutomatedTranscription(transcriptionTypeEnum)
            && !automaticWorkflowTransitionRules.get(currentTranscriptionStatus).contains(desiredTargetTranscriptionStatus)) {
            handleInvalidTranscriptionWorkflow(transcriptionTypeEnum, currentTranscriptionStatus, desiredTargetTranscriptionStatus);
        } else if (!manualWorkflowTransitionRules.get(currentTranscriptionStatus).contains(desiredTargetTranscriptionStatus)) {
            handleInvalidTranscriptionWorkflow(transcriptionTypeEnum, currentTranscriptionStatus, desiredTargetTranscriptionStatus);
        }

    }


    private void handleInvalidTranscriptionWorkflow(TranscriptionTypeEnum transcriptionTypeEnum,
                                                    TranscriptionStatusEnum currentTranscriptionStatus,
                                                    TranscriptionStatusEnum desiredTargetTranscriptionStatus) {

        log.warn("Unable to go from workflow state {} to {} for type", currentTranscriptionStatus, desiredTargetTranscriptionStatus, transcriptionTypeEnum);
        throw new DartsApiException(TranscriptionApiError.FAILED_TO_VALIDATE_TRANSCRIPTION_REQUEST);
    }

}
