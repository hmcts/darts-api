package uk.gov.hmcts.darts.transcriptions.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;

@Component
@RequiredArgsConstructor
public class TranscriptionDocumentIdValidator implements Validator<Long> {

    private final TranscriptionDocumentRepository documentRepository;

    @Override
    public void validate(Long id) {
        if (documentRepository.findById(id).isEmpty()) {
            throw new DartsApiException(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_ID_NOT_FOUND);
        }
    }
}