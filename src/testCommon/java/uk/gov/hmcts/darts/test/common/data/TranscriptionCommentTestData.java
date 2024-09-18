package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.test.common.data.builder.CustomTranscriptionCommentEntity;

import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

@SuppressWarnings({"HideUtilityClassConstructor"})
public class TranscriptionCommentTestData implements Persistable<CustomTranscriptionCommentEntity.CustomTranscriptionCommentEntityBuilderRetrieve> {

    TranscriptionCommentTestData() {
    }

    @Override
    public CustomTranscriptionCommentEntity.CustomTranscriptionCommentEntityBuilderRetrieve someMinimal() {
        CustomTranscriptionCommentEntity.CustomTranscriptionCommentEntityBuilderRetrieve retrieve
            = new CustomTranscriptionCommentEntity.CustomTranscriptionCommentEntityBuilderRetrieve();
        retrieve.getBuilder().lastModifiedBy(minimalUserAccount());
        retrieve.getBuilder().createdBy(minimalUserAccount());
        retrieve.getBuilder().transcription(PersistableFactory.getTranscriptionTestData().minimalTranscription());
        return retrieve;
    }

    @Override
    public CustomTranscriptionCommentEntity.CustomTranscriptionCommentEntityBuilderRetrieve someMaximal() {
        return someMinimal();
    }
}