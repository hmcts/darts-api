package uk.gov.hmcts.darts.audio.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.enums.HiddenReason;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.ObjectHiddenReasonRepository;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MediaApproveMarkForDeletionValidator implements Validator<Integer> {

    private final MediaIdValidator mediaIdValidator;
    private final ObjectAdminActionRepository objectAdminActionRepository;
    private final ObjectHiddenReasonRepository objectHiddenReasonRepository;
    private final UserIdentity userIdentity;

    @Override
    public void validate(Integer mediaId) {
        mediaIdValidator.validate(mediaId);
        var objectAdminActionEntityList = objectAdminActionRepository.findByMedia_Id(mediaId);
        if (objectAdminActionEntityList.isEmpty()) {
            throw new DartsApiException(AudioApiError.MEDIA_MARKED_FOR_DELETION_REASON_NOT_FOUND);
        }
        var objectAdminActionEntity = objectAdminActionEntityList.getFirst();
        if (objectAdminActionEntity.isMarkedForManualDeletion()) {
            throw new DartsApiException(AudioApiError.MEDIA_ALREADY_MARKED_FOR_DELETION);
        }

        Optional<ObjectHiddenReasonEntity> optionalObjectHiddenReasonEntity = objectHiddenReasonRepository.findById(
            HiddenReason.OTHER_DELETE.getId());
        if (optionalObjectHiddenReasonEntity.isEmpty()) {
            throw new DartsApiException(AudioApiError.MEDIA_HIDE_ACTION_REASON_NOT_FOUND);
        }
        var currentUser = userIdentity.getUserAccount();
        if (objectAdminActionEntity.) {

        }

    }
}
