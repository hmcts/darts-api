package uk.gov.hmcts.darts.common.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TranscriptionCommentEntityTest {

    @Test
    void positiveAnonymize() {
        TranscriptionCommentEntity transcriptionCommentEntity = new TranscriptionCommentEntity();
        transcriptionCommentEntity.setComment("comment");

        UserAccountEntity userAccount = new UserAccountEntity();
        UUID uuid = UUID.randomUUID();

        transcriptionCommentEntity.anonymize(userAccount, uuid);
        assertThat(transcriptionCommentEntity.getComment()).isEqualTo(uuid.toString());
        assertThat(transcriptionCommentEntity.getLastModifiedBy()).isEqualTo(userAccount);
        assertThat(transcriptionCommentEntity.isDataAnonymised()).isFalse();//This is only set for manual anonymization

    }
}
