package uk.gov.hmcts.darts.test.common.data.builder;

import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.AssertionFailure;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;

public class CustomTranscriptionCommentEntity extends TranscriptionCommentEntity implements DbInsertable<TranscriptionCommentEntity> {

    @lombok.Builder
    public CustomTranscriptionCommentEntity(
        Integer id,
        TranscriptionWorkflowEntity transcriptionWorkflow,
        TranscriptionEntity transcription,
        String legacyTranscriptionObjectId,
        String comment,
        OffsetDateTime commentTimestamp,
        Integer authorUserId,
        boolean isMigrated,
        boolean isDataAnonymised,
        OffsetDateTime createdDateTime,
        UserAccountEntity createdBy,
        OffsetDateTime lastModifiedDateTime,
        UserAccountEntity lastModifiedBy
    ) {
        // Set parent properties
        setId(id);
        setTranscriptionWorkflow(transcriptionWorkflow);
        setTranscription(transcription);
        setLegacyTranscriptionObjectId(legacyTranscriptionObjectId);
        setComment(comment);
        setCommentTimestamp(commentTimestamp);
        setAuthorUserId(authorUserId);
        setMigrated(isMigrated);
        setDataAnonymised(isDataAnonymised);
        setCreatedDateTime(createdDateTime);
        setCreatedBy(createdBy);
        setLastModifiedDateTime(lastModifiedDateTime);
        setLastModifiedBy(lastModifiedBy);
    }

    @Override
    public TranscriptionCommentEntity getEntity() {
        try {
            TranscriptionCommentEntity transcriptionCommentEntity = new TranscriptionCommentEntity();
            BeanUtils.copyProperties(transcriptionCommentEntity, this);
            return transcriptionCommentEntity;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionFailure("Assumed that there would be no error on mapping data", e);
        }
    }

    public static class CustomTranscriptionCommentEntityBuilderRetrieve
        implements BuilderHolder<CustomTranscriptionCommentEntity, CustomTranscriptionCommentEntity.CustomTranscriptionCommentEntityBuilder> {

        private CustomTranscriptionCommentEntity.CustomTranscriptionCommentEntityBuilder builder = CustomTranscriptionCommentEntity.builder();

        @Override
        public CustomTranscriptionCommentEntity build() {
            return builder.build();
        }

        @Override
        public CustomTranscriptionCommentEntity.CustomTranscriptionCommentEntityBuilder getBuilder() {
            return builder;
        }
    }
}