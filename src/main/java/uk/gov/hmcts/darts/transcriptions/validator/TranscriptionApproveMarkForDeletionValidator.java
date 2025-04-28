package uk.gov.hmcts.darts.transcriptions.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.ObjectHiddenReasonRepository;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;

import static java.util.Objects.isNull;

@Component
@RequiredArgsConstructor
public class TranscriptionApproveMarkForDeletionValidator implements Validator<Long> {

    private final ObjectAdminActionRepository objectAdminActionRepository;
    private final ObjectHiddenReasonRepository objectHiddenReasonRepository;
    private final UserIdentity userIdentity;
    private final TranscriptionDocumentIdValidator transcriptionDocumentIdValidator;

    @Override
    @SuppressWarnings("PMD.CyclomaticComplexity")//TODO - refactor to reduce complexity when this is next edited
    public void validate(Long transcriptionDocumentId) {
        transcriptionDocumentIdValidator.validate(transcriptionDocumentId);

        var objectAdminActionEntityList = objectAdminActionRepository.findByTranscriptionDocumentId(transcriptionDocumentId);
        if (objectAdminActionEntityList.isEmpty()) {
            throw new DartsApiException(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_ID_NOT_FOUND);
        }

        if (objectAdminActionEntityList.size() > 1) {
            throw new DartsApiException(TranscriptionApiError.TOO_MANY_RESULTS);
        }

        var objectAdminActionEntity = objectAdminActionEntityList.getFirst();
        if (objectAdminActionEntity.isMarkedForManualDeletion()) {
            throw new DartsApiException(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_DELETION_ALREADY_APPROVED);
        }

        if (objectAdminActionEntity.getObjectHiddenReason() != null) {
            ObjectHiddenReasonEntity objectHiddenReasonEntity =
                objectHiddenReasonRepository.findById(objectAdminActionEntity.getObjectHiddenReason().getId())
                    .orElseThrow(() -> new DartsApiException(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_MARKED_FOR_DELETION_REASON_NOT_FOUND));
            if (!objectHiddenReasonEntity.isMarkedForDeletion()) {
                throw new DartsApiException(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_MARKED_FOR_DELETION_REASON_NOT_FOUND);
            }
            UserAccountEntity currentUser = userIdentity.getUserAccount();
            UserAccountEntity hiddenBy = objectAdminActionEntity.getHiddenBy();
            if (isNull(hiddenBy) || currentUser.getId().equals(hiddenBy.getId())) {
                throw new DartsApiException(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_DELETION_CAN_NOT_APPROVE_OWN_REQUEST);
            }
        }
    }
}
