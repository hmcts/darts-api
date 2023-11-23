package uk.gov.hmcts.darts.transcriptions.validator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.CLOSED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.COMPLETE;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REJECTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;

@Component
@Slf4j
public class WorkflowValidator {

    private final Map<TranscriptionStatusEnum, Set<TranscriptionStatusEnum>> manualWorkflowTransitionRules =
        new EnumMap<>(TranscriptionStatusEnum.class);

    private final Map<TranscriptionStatusEnum, Set<TranscriptionStatusEnum>> automaticWorkflowTransitionRules =
        new EnumMap<>(TranscriptionStatusEnum.class);

    public WorkflowValidator() {
        manualWorkflowTransitionRules.put(REQUESTED, Set.of(AWAITING_AUTHORISATION, CLOSED));
        manualWorkflowTransitionRules.put(AWAITING_AUTHORISATION, Set.of(APPROVED, REJECTED, CLOSED));
        manualWorkflowTransitionRules.put(APPROVED, Set.of(WITH_TRANSCRIBER, CLOSED));
        manualWorkflowTransitionRules.put(REJECTED, Collections.emptySet());
        manualWorkflowTransitionRules.put(WITH_TRANSCRIBER, Set.of(COMPLETE, CLOSED));
        manualWorkflowTransitionRules.put(COMPLETE, Collections.emptySet());
        manualWorkflowTransitionRules.put(CLOSED, Collections.emptySet());

        automaticWorkflowTransitionRules.put(REQUESTED, Set.of(APPROVED, CLOSED));
        automaticWorkflowTransitionRules.put(APPROVED, Set.of(WITH_TRANSCRIBER, CLOSED));
        automaticWorkflowTransitionRules.put(WITH_TRANSCRIBER, Set.of(COMPLETE, CLOSED));
        automaticWorkflowTransitionRules.put(COMPLETE, Collections.emptySet());
        automaticWorkflowTransitionRules.put(CLOSED, Collections.emptySet());
    }




    public boolean validateChangeToWorkflowStatus(boolean isManual, TranscriptionTypeEnum transcriptionTypeEnum,
                                                  TranscriptionStatusEnum currentTranscriptionStatus,
                                                  TranscriptionStatusEnum desiredTargetTranscriptionStatus) {
        if (isManual) {
            return isValid(manualWorkflowTransitionRules, currentTranscriptionStatus, desiredTargetTranscriptionStatus, transcriptionTypeEnum,
                           isManual);
        }
        return isValid(automaticWorkflowTransitionRules, currentTranscriptionStatus, desiredTargetTranscriptionStatus, transcriptionTypeEnum,
                       isManual);
    }

    private boolean isValid(Map<TranscriptionStatusEnum, Set<TranscriptionStatusEnum>> transitionRules,
                            TranscriptionStatusEnum currentTranscriptionStatus,
                            TranscriptionStatusEnum desiredTargetTranscriptionStatus,
                            TranscriptionTypeEnum transcriptionTypeEnum, boolean isManual) {
        boolean isValid = transitionRules.get(currentTranscriptionStatus) != null
                          && transitionRules.get(currentTranscriptionStatus).contains(desiredTargetTranscriptionStatus);
        if (!isValid) {
            log.warn("Unable to go from workflow state {} to {} for type {} for isManual {}",
                     currentTranscriptionStatus,
                     desiredTargetTranscriptionStatus,
                     transcriptionTypeEnum, isManual);
        }
        return isValid;
    }

}
