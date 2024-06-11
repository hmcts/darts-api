package uk.gov.hmcts.darts.test.common.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import static uk.gov.hmcts.darts.test.common.data.TranscriptionTestData.minimalTranscription;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class TranscriptionWorkflowTestData {

    public static TranscriptionWorkflowEntity minimalTranscriptionWorkflow() {
        var transcriptionWorkflow = new TranscriptionWorkflowEntity();
        transcriptionWorkflow.setTranscription(minimalTranscription());
        transcriptionWorkflow.setWorkflowActor(minimalUserAccount());
        transcriptionWorkflow.setTranscriptionStatus(
            new TranscriptionStatusEntity(TranscriptionStatusEnum.REQUESTED.getId()));
        transcriptionWorkflow.setWorkflowTimestamp(OffsetDateTime.now());
        return transcriptionWorkflow;
    }

    public static TranscriptionWorkflowEntity workflowForTranscription(TranscriptionEntity transcription) {
        var transcriptionWorkflow = minimalTranscriptionWorkflow();
        transcription.setTranscriptionStatus(transcription.getTranscriptionStatus());
        transcriptionWorkflow.setTranscription(transcription);
        var workflowEntityList = new ArrayList<TranscriptionWorkflowEntity>();
        workflowEntityList.add(transcriptionWorkflow);
        transcription.setTranscriptionWorkflowEntities(workflowEntityList);
        return transcriptionWorkflow;
    }

    public static TranscriptionWorkflowEntity workflowForTranscriptionWithStatus(TranscriptionEntity transcription, TranscriptionStatusEnum status) {
        var transcriptionWorkflow = workflowForTranscription(transcription);
        transcriptionWorkflow.setTranscriptionStatus(new TranscriptionStatusEntity(status.getId()));
        return transcriptionWorkflow;
    }

}
