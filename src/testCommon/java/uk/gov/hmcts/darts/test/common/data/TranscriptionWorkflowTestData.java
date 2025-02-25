package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.test.common.data.builder.TestTranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;

import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

public final class TranscriptionWorkflowTestData implements Persistable<
    TestTranscriptionWorkflowEntity.TestTranscriptionWorkflowEntityBuilderRetrieve,
    TranscriptionWorkflowEntity, TestTranscriptionWorkflowEntity.TestTranscriptionWorkflowEntityBuilder> {

    public static final OffsetDateTime WORKFLOW_TIMESTAMP = OffsetDateTime.of(2025, 1, 10, 10,
                                                                              0, 0, 0, ZoneOffset.UTC);

    TranscriptionWorkflowTestData() {

    }

    public TranscriptionWorkflowEntity minimalTranscriptionWorkflow(TranscriptionStatusEnum transcriptionStatusEnum) {
        var transcriptionWorkflow = new TranscriptionWorkflowEntity();
        transcriptionWorkflow.setTranscription(PersistableFactory.getTranscriptionTestData().minimalTranscription());
        transcriptionWorkflow.setWorkflowActor(minimalUserAccount());
        transcriptionWorkflow.setTranscriptionStatus(
            new TranscriptionStatusEntity(transcriptionStatusEnum.getId()));
        transcriptionWorkflow.setWorkflowTimestamp(WORKFLOW_TIMESTAMP);
        return transcriptionWorkflow;
    }

    public TranscriptionWorkflowEntity workflowForTranscription(TranscriptionEntity transcription, TranscriptionStatusEnum transcriptionStatusEnum) {
        var transcriptionWorkflow = minimalTranscriptionWorkflow(transcriptionStatusEnum);
        transcription.setTranscriptionStatus(transcription.getTranscriptionStatus());
        transcriptionWorkflow.setTranscription(transcription);
        var workflowEntityList = new ArrayList<TranscriptionWorkflowEntity>();
        workflowEntityList.add(transcriptionWorkflow);
        transcription.setTranscriptionWorkflowEntities(workflowEntityList);
        return transcriptionWorkflow;
    }

    public TranscriptionWorkflowEntity workflowForTranscriptionWithStatus(TranscriptionEntity transcription, TranscriptionStatusEnum status) {
        var transcriptionWorkflow = workflowForTranscription(transcription, status);
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