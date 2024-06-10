package uk.gov.hmcts.darts.transcriptions.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.ObjectHiddenReasonRepository;
import uk.gov.hmcts.darts.common.validation.IdRequest;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentHideRequest;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TranscriptionDocumentHideOrShowValidator implements Validator<IdRequest<TranscriptionDocumentHideRequest>> {

    private final ObjectAdminActionRepository objectAdminActionRepository;
    private final TranscriptionDocumentIdValidator transcriptionDocumentIdValidator;
    private final ObjectHiddenReasonRepository objectHiddenReasonRepository;

    @Override
    @SuppressWarnings("java:S5411")
    public void validate(IdRequest<TranscriptionDocumentHideRequest> request) {
         transcriptionDocumentIdValidator.validate(request.getId());

         if (request.getPayload().getIsHidden() && request.getPayload().getAdminAction() == null) {
             throw new DartsApiException(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_HIDE_ACTION_PAYLOAD_INCORRECT_USAGE);
         } else if (request.getPayload().getIsHidden()) {
             List<ObjectAdminActionEntity> objectAdminActionEntityList = objectAdminActionRepository.findByTranscriptionDocument_Id(request.getId());
             if (!objectAdminActionEntityList.isEmpty()) {
                 throw new DartsApiException(TranscriptionApiError.TRANSCRIPTION_ALREADY_HIDDEN);
             }
         }

        if (!request.getPayload().getIsHidden() && request.getPayload().getAdminAction() != null) {
            throw new DartsApiException(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_SHOW_ACTION_PAYLOAD_INCORRECT_USAGE);
        }

        if (request.getPayload().getAdminAction() != null && request.getPayload().getAdminAction().getReasonId() != null
                && objectHiddenReasonRepository.findById(request.getPayload().getAdminAction().getReasonId()).isEmpty()) {
                throw new DartsApiException(TranscriptionApiError
                                                .TRANSCRIPTION_DOCUMENT_HIDE_ACTION_REASON_NOT_FOUND);
        }
    }
}