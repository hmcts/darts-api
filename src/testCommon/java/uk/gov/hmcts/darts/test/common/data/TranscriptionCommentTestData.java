package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.test.common.data.builder.TestTranscriptionCommentEntity;

import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

public final class TranscriptionCommentTestData
    implements Persistable<TestTranscriptionCommentEntity.TestTranscriptionCommentEntityBuilderRetrieve, TranscriptionCommentEntity,
    TestTranscriptionCommentEntity.TestTranscriptionCommentEntityBuilder> {

    TranscriptionCommentTestData() {
        // This constructor is intentionally empty. Nothing special is needed here.
    }

    @Override
    public TranscriptionCommentEntity someMinimal() {
        return someMinimalBuilderHolder().getBuilder().build().getEntity();
    }

    @Override
    public TestTranscriptionCommentEntity.TestTranscriptionCommentEntityBuilderRetrieve someMinimalBuilderHolder() {
        TestTranscriptionCommentEntity.TestTranscriptionCommentEntityBuilderRetrieve retrieve
            = new TestTranscriptionCommentEntity.TestTranscriptionCommentEntityBuilderRetrieve();
        retrieve.getBuilder().lastModifiedBy(minimalUserAccount());
        retrieve.getBuilder().createdBy(minimalUserAccount());
        retrieve.getBuilder().transcription(PersistableFactory.getTranscriptionTestData().minimalTranscription());
        return retrieve;
    }

    @Override
    public TestTranscriptionCommentEntity.TestTranscriptionCommentEntityBuilder someMinimalBuilder() {
        return someMinimalBuilderHolder().getBuilder();
    }
}