package uk.gov.hmcts.darts.audio.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.model.AdminActionRequest;
import uk.gov.hmcts.darts.audio.model.MediaHideRequest;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.ObjectHiddenReasonRepository;
import uk.gov.hmcts.darts.common.validation.IdRequest;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MediaHideOrShowValidator implements Validator<IdRequest<MediaHideRequest>> {

    private final ObjectAdminActionRepository objectAdminActionRepository;
    private final MediaIdValidator mediaIdValidator;
    private final ObjectHiddenReasonRepository objectHiddenReasonRepository;

    @Override
    @SuppressWarnings("java:S5411")
    public void validate(IdRequest<MediaHideRequest> request) {
        mediaIdValidator.validate(request.getId());

        AdminActionRequest adminActionRequest = request.getPayload().getAdminAction();

        if (request.getPayload().getIsHidden() && adminActionRequest  == null) {
             throw new DartsApiException(AudioApiError.MEDIA_HIDE_ACTION_PAYLOAD_INCORRECT_USAGE);
         } else if (request.getPayload().getIsHidden()) {
             List<ObjectAdminActionEntity> objectAdminActionEntityList = objectAdminActionRepository.findByMedia_Id(request.getId());
             if (!objectAdminActionEntityList.isEmpty()) {
                 throw new DartsApiException(AudioApiError.MEDIA_ALREADY_HIDDEN);
             }
         }

        if (!request.getPayload().getIsHidden() && adminActionRequest != null) {
            throw new DartsApiException(AudioApiError.MEDIA_SHOW_ACTION_PAYLOAD_INCORRECT_USAGE);
        }


        if (adminActionRequest != null && adminActionRequest.getReasonId() != null) {
            Optional<ObjectHiddenReasonEntity> optionalObjectHiddenReasonEntity = objectHiddenReasonRepository.findById(
                adminActionRequest.getReasonId());
            if (optionalObjectHiddenReasonEntity.isEmpty()) {
                throw new DartsApiException(AudioApiError
                                                .MEDIA_HIDE_ACTION_REASON_NOT_FOUND);
            } else {
                ObjectHiddenReasonEntity objectHiddenReasonEntity = optionalObjectHiddenReasonEntity.get();
                if (objectHiddenReasonEntity.isMarkedForDeletion()) {
                    throw new DartsApiException(AudioApiError
                                                    .REASON_IS_MARKED_FOR_DELETION);
                }
            }
        }
    }
}