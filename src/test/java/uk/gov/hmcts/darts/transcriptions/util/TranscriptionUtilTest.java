package uk.gov.hmcts.darts.transcriptions.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranscriptionUtilTest {

    @Mock
    TranscriptionEntity transcriptionEntity;

    @Mock
    UserAccountEntity userAccountEntity;

    @Test
    void getRequestedByNameWithValidRequestedBy() {

        when(transcriptionEntity.getRequestedBy()).thenReturn(userAccountEntity);
        when(userAccountEntity.getUserFullName()).thenReturn("John Doe");

        String result = TranscriptionUtil.getRequestedByName(transcriptionEntity);

        assertEquals("John Doe", result);
    }

    @Test
    void getRequestedByNameWithNullRequestedBy() {

        when(transcriptionEntity.getRequestedBy()).thenReturn(null);

        String result = TranscriptionUtil.getRequestedByName(transcriptionEntity);

        assertNull(result);
    }

    @Test
    void getRequestedByNameWithNullTranscriptionEntityThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            TranscriptionUtil.getRequestedByName(null);
        });
    }
}
