package uk.gov.hmcts.darts.audio.validation;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.model.AdminActionRequest;
import uk.gov.hmcts.darts.audio.model.MediaHideRequest;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.ObjectHiddenReasonRepository;
import uk.gov.hmcts.darts.common.validation.IdRequest;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MediaHideOrShowValidator implements Validator<IdRequest<MediaHideRequest, Long>> {

    private final ObjectAdminActionRepository objectAdminActionRepository;
    private final MediaIdValidator mediaIdValidator;
    private final ObjectHiddenReasonRepository objectHiddenReasonRepository;

    @Value("${darts.manual-deletion.enabled:false}")
    @Getter(AccessLevel.PACKAGE)
    private boolean manualDeletionEnabled;

    @Override
    @SuppressWarnings({
        "java:S5411",
        "PMD.CyclomaticComplexity"//TODO - refactor to reduce complexity when this is next edited

    })

    public void validate(IdRequest<MediaHideRequest, Long> request) {
        mediaIdValidator.validate(request.getId());

        AdminActionRequest adminActionRequest = request.getPayload().getAdminAction();

        if (request.getPayload().getIsHidden() && adminActionRequest == null) {
            throw new DartsApiException(AudioApiError.MEDIA_HIDE_ACTION_PAYLOAD_INCORRECT_USAGE);
        } else if (request.getPayload().getIsHidden()) {
            List<ObjectAdminActionEntity> objectAdminActionEntityList = objectAdminActionRepository.findByMediaId(request.getId());
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
                throw new DartsApiException(AudioApiError.MEDIA_HIDE_ACTION_REASON_NOT_FOUND);
            } else if (!isManualDeletionEnabled() && optionalObjectHiddenReasonEntity.get().isMarkedForDeletion()) {
                throw new DartsApiException(CommonApiError.FEATURE_FLAG_NOT_ENABLED, "Manual deletion is not enabled");
            }
        }
    }
}