package uk.gov.hmcts.darts.transcriptions.validator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;

import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranscriptionDocumentIdValidatorTest {

    @Mock
    private TranscriptionDocumentRepository transcriptionDocumentRepository;

    @InjectMocks
    private TranscriptionDocumentIdValidator transcriptionDocumentIdValidator;

    @Test
    void successfullyGetId() {
        Integer documentId = 200;
        TranscriptionDocumentEntity documentEntity = new TranscriptionDocumentEntity();
        when(transcriptionDocumentRepository.findById(documentId)).thenReturn(Optional.of(documentEntity));

        transcriptionDocumentIdValidator.validate(documentId);
    }

    @Test
    void failureGettingId() {
        Integer documentId = 200;
        when(transcriptionDocumentRepository.findById(documentId)).thenReturn(Optional.empty());

        DartsApiException exception = Assertions.assertThrows(DartsApiException.class, () -> transcriptionDocumentIdValidator.validate(documentId));
        Assertions.assertEquals(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_ID_NOT_FOUND, exception.getError());
    }
}