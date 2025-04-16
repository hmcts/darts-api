package uk.gov.hmcts.darts.audio.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.ObjectHiddenReasonRepository;

import static java.util.Objects.isNull;

@Component
@RequiredArgsConstructor
public class MediaApproveMarkForDeletionValidator implements Validator<Integer> {

    private final MediaIdValidator mediaIdValidator;
    private final ObjectAdminActionRepository objectAdminActionRepository;
    private final ObjectHiddenReasonRepository objectHiddenReasonRepository;
    private final UserIdentity userIdentity;

    @Override
    @SuppressWarnings("PMD.CyclomaticComplexity")//TODO - refactor to reduce complexity when this is next edited
    public void validate(Integer mediaId) {
        mediaIdValidator.validate(mediaId);
        var objectAdminActionEntityList = objectAdminActionRepository.findByMediaId(mediaId);
        if (objectAdminActionEntityList.isEmpty()) {
            throw new DartsApiException(AudioApiError.ADMIN_MEDIA_MARKED_FOR_DELETION_NOT_FOUND);
        } else if (objectAdminActionEntityList.size() > 1) {
            throw new DartsApiException(AudioApiError.TOO_MANY_RESULTS);
        }

        var objectAdminActionEntity = objectAdminActionEntityList.getFirst();
        if (objectAdminActionEntity.isMarkedForManualDeletion()) {
            throw new DartsApiException(AudioApiError.MEDIA_ALREADY_MARKED_FOR_DELETION);
        }

        if (objectAdminActionEntity.getObjectHiddenReason() == null) {
            throw new DartsApiException(AudioApiError.MEDIA_MARKED_FOR_DELETION_REASON_NOT_FOUND);
        }
        ObjectHiddenReasonEntity objectHiddenReasonEntity =
            objectHiddenReasonRepository.findById(objectAdminActionEntity.getObjectHiddenReason().getId())
                .orElseThrow(() -> new DartsApiException(AudioApiError.MEDIA_MARKED_FOR_DELETION_REASON_NOT_FOUND));
        if (!objectHiddenReasonEntity.isMarkedForDeletion()) {
            throw new DartsApiException(AudioApiError.MEDIA_MARKED_FOR_DELETION_REASON_NOT_FOUND);
        }
        UserAccountEntity currentUser = userIdentity.getUserAccount();
        UserAccountEntity hiddenBy = objectAdminActionEntity.getHiddenBy();
        if (isNull(hiddenBy)
            || hiddenBy.getId().equals(currentUser.getId())) {
            throw new DartsApiException(AudioApiError.USER_CANNOT_APPROVE_THEIR_OWN_DELETION);
        }
    }
}
