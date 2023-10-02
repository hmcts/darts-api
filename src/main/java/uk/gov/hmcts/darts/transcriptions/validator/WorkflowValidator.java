package uk.gov.hmcts.darts.transcriptions.validator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;

import java.util.Collections;
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

    // Create a map defining the transition rules, from any given current state to a list of allowable target states
    private final Map<TranscriptionStatusEnum, Set<TranscriptionStatusEnum>> manualWorkflowTransitionRules = new HashMap<>();

    private final Map<TranscriptionStatusEnum, Set<TranscriptionStatusEnum>> automaticWorkflowTransitionRules = new HashMap<>();

    public WorkflowValidator() {
        manualWorkflowTransitionRules.put(REQUESTED, Set.of(REQUESTED, AWAITING_AUTHORISATION, CLOSED));
        manualWorkflowTransitionRules.put(AWAITING_AUTHORISATION, Set.of(APPROVED, REJECTED, CLOSED));
        manualWorkflowTransitionRules.put(APPROVED, Set.of(WITH_TRANSCRIBER, CLOSED));
        manualWorkflowTransitionRules.put(REJECTED, Collections.emptySet());
        manualWorkflowTransitionRules.put(WITH_TRANSCRIBER, Set.of(COMPLETE, CLOSED));
        manualWorkflowTransitionRules.put(COMPLETE, Collections.emptySet());
        manualWorkflowTransitionRules.put(CLOSED, Collections.emptySet());

        automaticWorkflowTransitionRules.put(REQUESTED, Set.of(REQUESTED, APPROVED, CLOSED));
        automaticWorkflowTransitionRules.put(AWAITING_AUTHORISATION, Collections.emptySet());
        automaticWorkflowTransitionRules.put(APPROVED, Set.of(WITH_TRANSCRIBER, CLOSED));
        automaticWorkflowTransitionRules.put(REJECTED, Collections.emptySet());
        automaticWorkflowTransitionRules.put(WITH_TRANSCRIBER, Set.of(COMPLETE, CLOSED));
        automaticWorkflowTransitionRules.put(COMPLETE, Collections.emptySet());
        automaticWorkflowTransitionRules.put(CLOSED, Collections.emptySet());
    }

    public boolean isAutomatedTranscription(TranscriptionTypeEnum transcriptionTypeEnum) {
        return OTHER.equals(transcriptionTypeEnum);
    }

    public boolean validateChangeToWorkflowStatus(TranscriptionTypeEnum transcriptionTypeEnum,
                                               TranscriptionStatusEnum currentTranscriptionStatus,
                                               TranscriptionStatusEnum desiredTargetTranscriptionStatus) {
        if (isAutomatedTranscription(transcriptionTypeEnum)) {
            if (!automaticWorkflowTransitionRules.get(currentTranscriptionStatus).contains(desiredTargetTranscriptionStatus)) {
                handleInvalidTranscriptionWorkflow(transcriptionTypeEnum, currentTranscriptionStatus, desiredTargetTranscriptionStatus);
            }
        } else if (!manualWorkflowTransitionRules.get(currentTranscriptionStatus).contains(desiredTargetTranscriptionStatus)) {
            handleInvalidTranscriptionWorkflow(transcriptionTypeEnum, currentTranscriptionStatus, desiredTargetTranscriptionStatus);
        }
        return true;
    }


    private void handleInvalidTranscriptionWorkflow(TranscriptionTypeEnum transcriptionTypeEnum,
                                                    TranscriptionStatusEnum currentTranscriptionStatus,
                                                    TranscriptionStatusEnum desiredTargetTranscriptionStatus) {

        log.warn("Unable to go from workflow state {} to {} for type {}", currentTranscriptionStatus, desiredTargetTranscriptionStatus, transcriptionTypeEnum);
        throw new DartsApiException(TranscriptionApiError.FAILED_TO_VALIDATE_TRANSCRIPTION_REQUEST);
    }

}
