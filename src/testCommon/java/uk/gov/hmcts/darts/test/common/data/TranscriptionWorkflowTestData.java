package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

@SuppressWarnings({"HideUtilityClassConstructor"})
public class TranscriptionWorkflowTestData  implements Persistable<CustomTranscriptionWorkflowEntity.CustomTranscriptionWorkflowEntityBuilderRetrieve> {

    TranscriptionWorkflowTestData() {

    }

    public TranscriptionWorkflowEntity minimalTranscriptionWorkflow() {
        return someMinimal().build();
    }

    public TranscriptionWorkflowEntity workflowForTranscription(TranscriptionEntity transcription) {
        var transcriptionWorkflow = minimalTranscriptionWorkflow();
        transcription.setTranscriptionStatus(transcription.getTranscriptionStatus());
        transcriptionWorkflow.setTranscription(transcription);
        var workflowEntityList = new ArrayList<TranscriptionWorkflowEntity>();
        workflowEntityList.add(transcriptionWorkflow);
        transcription.setTranscriptionWorkflowEntities(workflowEntityList);
        return transcriptionWorkflow;
    }

    public TranscriptionWorkflowEntity workflowForTranscriptionWithStatus(TranscriptionEntity transcription, TranscriptionStatusEnum status) {
        var transcriptionWorkflow = workflowForTranscription(transcription);
        transcriptionWorkflow.setTranscriptionStatus(new TranscriptionStatusEntity(status.getId()));
        return transcriptionWorkflow;
    }

    @Override
    public CustomTranscriptionWorkflowEntity.CustomTranscriptionWorkflowEntityBuilderRetrieve someMinimal() {
        CustomTranscriptionWorkflowEntity.CustomTranscriptionWorkflowEntityBuilderRetrieve retrieve =
            new CustomTranscriptionWorkflowEntity.CustomTranscriptionWorkflowEntityBuilderRetrieve();

        retrieve.getBuilder().transcription(PersistableFactory.getTranscriptionTestData().minimalTranscription())
            .workflowActor(minimalUserAccount())
            .transcriptionStatus(new TranscriptionStatusEntity(TranscriptionStatusEnum.REQUESTED.getId()))
            .workflowTimestamp(OffsetDateTime.now());

        return retrieve;
   }

    @Override
    public CustomTranscriptionWorkflowEntity.CustomTranscriptionWorkflowEntityBuilderRetrieve someMaximal() {
        return someMinimal();
    }
}