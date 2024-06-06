package uk.gov.hmcts.darts.transcriptions.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.common.validation.IdRequest;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentHideRequest;

@Component
@RequiredArgsConstructor
public class TranscriptionDocumentHideOrShowValidator implements Validator<IdRequest<TranscriptionDocumentHideRequest>> {

    private final TranscriptionDocumentRepository documentRepository;

    @Override
    public void validate(IdRequest<TranscriptionDocumentHideRequest> request) {
        if (!documentRepository.findById(request.getId()).isPresent()) {
            throw new DartsApiException(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_ID_NOT_FOUND);
        }
    }
}