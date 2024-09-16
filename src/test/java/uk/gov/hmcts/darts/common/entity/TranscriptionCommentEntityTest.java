package uk.gov.hmcts.darts.common.entity;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.test.common.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class TranscriptionCommentEntityTest {

    @Test
    void positiveAnonymize() {
        TranscriptionCommentEntity transcriptionCommentEntity = new TranscriptionCommentEntity();
        transcriptionCommentEntity.setComment("comment");

        UserAccountEntity userAccount = new UserAccountEntity();
        transcriptionCommentEntity.anonymize(userAccount);
        assertThat(transcriptionCommentEntity.getComment()).matches(TestUtils.UUID_REGEX);
        assertThat(transcriptionCommentEntity.getLastModifiedBy()).isEqualTo(userAccount);
        assertThat(transcriptionCommentEntity.isDataAnonymised()).isFalse();//This is only set for manual anonymization

    }
}
