package uk.gov.hmcts.darts.audio.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.exception.AudioRequestsApiError;
import uk.gov.hmcts.darts.audiorequests.model.MediaPatchRequest;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.validation.IdRequest;

@Component
@RequiredArgsConstructor
@Slf4j
public class AudioMediaPatchRequestValidator implements Validator<IdRequest<MediaPatchRequest, Integer>> {
    private final UserAccountRepository userAccountRepository;

    private final MediaRequestRepository mediaRequestRepository;

    @Override
    @SuppressWarnings({"PMD.CyclomaticComplexity"})
    public void validate(IdRequest<MediaPatchRequest, Integer> patchRequest) {
        if (patchRequest.getId() == null || !mediaRequestRepository.findById(patchRequest.getId()).isPresent()) {
            throw new DartsApiException(AudioRequestsApiError.MEDIA_REQUEST_NOT_FOUND);
        }

        if (patchRequest.getPayload() != null
            && patchRequest.getPayload().getOwnerId() != null
            && !userAccountRepository.findById(patchRequest.getPayload().getOwnerId()).isPresent()) {
            throw new DartsApiException(AudioRequestsApiError.USER_IS_NOT_FOUND);
        }
    }
}