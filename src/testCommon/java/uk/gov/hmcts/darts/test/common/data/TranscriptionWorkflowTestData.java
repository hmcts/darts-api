package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.test.common.data.builder.TestTranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

public class TranscriptionWorkflowTestData implements Persistable<
    TestTranscriptionWorkflowEntity.TestTranscriptionWorkflowEntityBuilderRetrieve,
    TranscriptionWorkflowEntity, TestTranscriptionWorkflowEntity.TestTranscriptionWorkflowEntityBuilder> {

    TranscriptionWorkflowTestData() {

    }

    public TranscriptionWorkflowEntity minimalTranscriptionWorkflow() {
        return someMinimal();
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
    public TranscriptionWorkflowEntity someMinimal() {
        return someMinimalBuilder().build().getEntity();
    }

    @Override
    public TestTranscriptionWorkflowEntity.TestTranscriptionWorkflowEntityBuilderRetrieve someMinimalBuilderHolder() {
        TestTranscriptionWorkflowEntity.TestTranscriptionWorkflowEntityBuilderRetrieve retrieve =
            new TestTranscriptionWorkflowEntity.TestTranscriptionWorkflowEntityBuilderRetrieve();

        retrieve.getBuilder().transcription(PersistableFactory.getTranscriptionTestData().minimalTranscription())
            .workflowActor(minimalUserAccount())
            .transcriptionStatus(new TranscriptionStatusEntity(TranscriptionStatusEnum.REQUESTED.getId()))
            .workflowTimestamp(OffsetDateTime.now());

        return retrieve;
    }

    @Override
    public TestTranscriptionWorkflowEntity.TestTranscriptionWorkflowEntityBuilder someMinimalBuilder() {
        return someMinimalBuilderHolder().getBuilder();
    }
}