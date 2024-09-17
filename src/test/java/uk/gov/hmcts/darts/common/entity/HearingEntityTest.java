package uk.gov.hmcts.darts.common.entity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class HearingEntityTest {

    @Test
    void positiveAnonymize() {
        HearingEntity hearingEntity = new HearingEntity();

        TranscriptionEntity transcriptionEntity1 = mock(TranscriptionEntity.class);
        TranscriptionEntity transcriptionEntity2 = mock(TranscriptionEntity.class);
        hearingEntity.setTranscriptions(List.of(transcriptionEntity1, transcriptionEntity2));

        EventEntity entityEntity1 = mock(EventEntity.class);
        EventEntity entityEntity2 = mock(EventEntity.class);
        hearingEntity.setEventList(List.of(entityEntity1, entityEntity2));

        UserAccountEntity userAccount = new UserAccountEntity();

        hearingEntity.anonymize(userAccount);
        verify(transcriptionEntity1, times(1)).anonymize(userAccount);
        verify(transcriptionEntity2, times(1)).anonymize(userAccount);
        verify(entityEntity1, times(1)).anonymize(userAccount);
        verify(entityEntity2, times(1)).anonymize(userAccount);
    }
}
