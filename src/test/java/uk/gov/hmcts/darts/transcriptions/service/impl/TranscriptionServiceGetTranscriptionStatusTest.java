package uk.gov.hmcts.darts.transcriptions.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.repository.TranscriptionStatusRepository;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranscriptionServiceGetTranscriptionStatusTest {

    @Mock
    private TranscriptionStatusRepository mockTranscriptionStatusRepository;

    @InjectMocks
    private TranscriptionServiceImpl transcriptionService;

    @Test
    void shouldSuccessfullyReturnListOfTranscriptionStatus() {

        when(mockTranscriptionStatusRepository.findAll()).thenReturn(getTranscriptionStatuses());

        List<TranscriptionStatus> transcriptionStatuses = transcriptionService.getTranscriptionStatuses();

        assertEquals(1, transcriptionStatuses.getFirst().getId());
        assertEquals("Requested", transcriptionStatuses.getFirst().getType());
        assertEquals("Requested", transcriptionStatuses.getFirst().getDisplayName());

        verify(mockTranscriptionStatusRepository, times(1)).findAll();

    }

    private List<TranscriptionStatusEntity> getTranscriptionStatuses() {

        TranscriptionStatusEntity transcriptionStatusEntity = new TranscriptionStatusEntity();

        transcriptionStatusEntity.setId(1);
        transcriptionStatusEntity.setStatusType("Requested");
        transcriptionStatusEntity.setDisplayName("Requested");

        return List.of(transcriptionStatusEntity);
    }
}


