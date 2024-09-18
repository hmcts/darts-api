package uk.gov.hmcts.darts.test.common.data;

import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.AssertionFailure;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.test.common.data.builder.BuilderHolder;
import uk.gov.hmcts.darts.test.common.data.builder.DbInsertable;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;
import java.util.List;

public class CustomTranscriptionWorkflowEntity extends TranscriptionWorkflowEntity implements DbInsertable<TranscriptionWorkflowEntity> {

    @lombok.Builder
    public CustomTranscriptionWorkflowEntity(
        Integer id,
        TranscriptionEntity transcription,
        TranscriptionStatusEntity transcriptionStatus,
        UserAccountEntity workflowActor,
        OffsetDateTime workflowTimestamp,
        List<TranscriptionCommentEntity> transcriptionComments
    ) {
        // Set parent properties
        setId(id);
        setTranscription(transcription);
        setTranscriptionStatus(transcriptionStatus);
        setWorkflowActor(workflowActor);
        setWorkflowTimestamp(workflowTimestamp);
        setTranscriptionComments(transcriptionComments);
    }

    @Override
    public TranscriptionWorkflowEntity getEntity() {
        try {
            TranscriptionWorkflowEntity workflowEntity = new TranscriptionWorkflowEntity();
            BeanUtils.copyProperties(workflowEntity, this);
            return workflowEntity;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionFailure("Assumed that there would be no error on mapping data", e);
        }
    }

    public static class CustomTranscriptionWorkflowEntityBuilderRetrieve
        implements BuilderHolder<CustomTranscriptionWorkflowEntity, CustomTranscriptionWorkflowEntity.CustomTranscriptionWorkflowEntityBuilder> {

        private CustomTranscriptionWorkflowEntity.CustomTranscriptionWorkflowEntityBuilder builder = CustomTranscriptionWorkflowEntity.builder();

        @Override
        public CustomTranscriptionWorkflowEntity build() {
            return builder.build();
        }

        @Override
        public CustomTranscriptionWorkflowEntity.CustomTranscriptionWorkflowEntityBuilder getBuilder() {
            return builder;
        }
    }
}