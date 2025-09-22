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
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.UNFULFILLED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;

@Component
@Slf4j
@SuppressWarnings({"PMD.NcssCount"})
public class WorkflowValidator {

    private final Map<TranscriptionStatusEnum, Set<TranscriptionStatusEnum>> manualWorkflowTransitionRules =
        new EnumMap<>(TranscriptionStatusEnum.class);

    private final Map<TranscriptionStatusEnum, Set<TranscriptionStatusEnum>> automaticWorkflowTransitionRules =
        new EnumMap<>(TranscriptionStatusEnum.class);

    private final Map<TranscriptionStatusEnum, Set<TranscriptionStatusEnum>> manualWorkflowTransitionRulesAdmin =
        new EnumMap<>(TranscriptionStatusEnum.class);

    private final Map<TranscriptionStatusEnum, Set<TranscriptionStatusEnum>> automaticWorkflowTransitionRulesAdmin =
        new EnumMap<>(TranscriptionStatusEnum.class);

    public WorkflowValidator() {
        manualWorkflowTransitionRules.put(REQUESTED, Set.of(AWAITING_AUTHORISATION, CLOSED));
        manualWorkflowTransitionRules.put(AWAITING_AUTHORISATION, Set.of(APPROVED, REJECTED, CLOSED));
        manualWorkflowTransitionRules.put(APPROVED, Set.of(WITH_TRANSCRIBER, CLOSED));
        manualWorkflowTransitionRules.put(REJECTED, Collections.emptySet());
        manualWorkflowTransitionRules.put(WITH_TRANSCRIBER, Set.of(COMPLETE, CLOSED, UNFULFILLED));
        manualWorkflowTransitionRules.put(COMPLETE, Collections.emptySet());
        manualWorkflowTransitionRules.put(CLOSED, Collections.emptySet());
        manualWorkflowTransitionRules.put(UNFULFILLED, Collections.emptySet());

        manualWorkflowTransitionRulesAdmin.put(REQUESTED, Set.of(CLOSED));
        manualWorkflowTransitionRulesAdmin.put(AWAITING_AUTHORISATION, Set.of(REQUESTED, CLOSED));
        manualWorkflowTransitionRulesAdmin.put(APPROVED, Set.of(CLOSED));
        manualWorkflowTransitionRulesAdmin.put(REJECTED, Collections.emptySet());
        manualWorkflowTransitionRulesAdmin.put(WITH_TRANSCRIBER, Set.of(APPROVED, CLOSED, UNFULFILLED));
        manualWorkflowTransitionRulesAdmin.put(COMPLETE, Collections.emptySet());
        manualWorkflowTransitionRulesAdmin.put(CLOSED, Collections.emptySet());
        manualWorkflowTransitionRulesAdmin.put(UNFULFILLED, Collections.emptySet());

        automaticWorkflowTransitionRules.put(REQUESTED, Set.of(APPROVED, CLOSED));
        automaticWorkflowTransitionRules.put(APPROVED, Set.of(WITH_TRANSCRIBER, CLOSED));
        automaticWorkflowTransitionRules.put(WITH_TRANSCRIBER, Set.of(COMPLETE, CLOSED, UNFULFILLED));
        automaticWorkflowTransitionRules.put(COMPLETE, Collections.emptySet());
        automaticWorkflowTransitionRules.put(CLOSED, Collections.emptySet());
        automaticWorkflowTransitionRules.put(UNFULFILLED, Collections.emptySet());

        automaticWorkflowTransitionRulesAdmin.put(REQUESTED, Set.of(CLOSED));
        automaticWorkflowTransitionRulesAdmin.put(APPROVED, Set.of(CLOSED));
        automaticWorkflowTransitionRulesAdmin.put(WITH_TRANSCRIBER, Set.of(APPROVED, COMPLETE, CLOSED, UNFULFILLED));
        automaticWorkflowTransitionRulesAdmin.put(COMPLETE, Collections.emptySet());
        automaticWorkflowTransitionRulesAdmin.put(CLOSED, Collections.emptySet());
        automaticWorkflowTransitionRulesAdmin.put(UNFULFILLED, Collections.emptySet());
    }

    public boolean validateChangeToWorkflowStatus(boolean isManual, TranscriptionTypeEnum transcriptionTypeEnum,
                                                  TranscriptionStatusEnum currentTranscriptionStatus,
                                                  TranscriptionStatusEnum desiredTargetTranscriptionStatus, boolean isAdmin) {

        Map<TranscriptionStatusEnum, Set<TranscriptionStatusEnum>> rulesToUse;
        if (isManual) {
            if (isAdmin) {
                rulesToUse = manualWorkflowTransitionRulesAdmin;
            } else {
                rulesToUse = manualWorkflowTransitionRules;
            }
        } else {
            if (isAdmin) {
                rulesToUse = automaticWorkflowTransitionRulesAdmin;
            } else {
                rulesToUse = automaticWorkflowTransitionRules;
            }
        }
        return isValid(rulesToUse, currentTranscriptionStatus, desiredTargetTranscriptionStatus, transcriptionTypeEnum,
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
