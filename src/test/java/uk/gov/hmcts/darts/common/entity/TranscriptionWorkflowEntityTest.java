package uk.gov.hmcts.darts.common.entity;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TranscriptionWorkflowEntityTest {

    @Test
    void close() {
        TranscriptionWorkflowEntity transcriptionWorkflowEntity = new TranscriptionWorkflowEntity();
        TranscriptionStatusEntity transcriptionStatus = mock(TranscriptionStatusEntity.class);
        transcriptionWorkflowEntity.setTranscriptionStatus(transcriptionStatus);
        transcriptionWorkflowEntity.close();
        assertThat(transcriptionWorkflowEntity.getTranscriptionStatus().getId())
            .isEqualTo(TranscriptionStatusEnum.CLOSED.getId());
    }
}
