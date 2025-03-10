package uk.gov.hmcts.darts.test.common.data.builder;

import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.AssertionFailure;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;

@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.ConstructorCallsOverridableMethod"})
public class TestTranscriptionCommentEntity extends TranscriptionCommentEntity implements DbInsertable<TranscriptionCommentEntity> {

    @lombok.Builder
    public TestTranscriptionCommentEntity(
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

    public static class TestTranscriptionCommentEntityBuilderRetrieve
        implements BuilderHolder<TestTranscriptionCommentEntity, TestTranscriptionCommentEntity.TestTranscriptionCommentEntityBuilder> {

        private TestTranscriptionCommentEntity.TestTranscriptionCommentEntityBuilder builder = TestTranscriptionCommentEntity.builder();

        @Override
        public TestTranscriptionCommentEntity build() {
            return builder.build();
        }

        @Override
        public TestTranscriptionCommentEntity.TestTranscriptionCommentEntityBuilder getBuilder() {
            return builder;
        }
    }
}