package uk.gov.hmcts.darts.audio.validation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.exception.AudioRequestsApiError;
import uk.gov.hmcts.darts.audiorequests.model.MediaPatchRequest;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.validation.IdRequest;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AudioMediaPatchRequestValidatorTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private MediaRequestRepository mediaRequestRepository;

    @InjectMocks
    private AudioMediaPatchRequestValidator audioMediaPatchRequestValidator;

    @Test
    void successfulPatchWithoutOwner() {
        Integer mediaRequestId = 200;
        MediaPatchRequest mediaPatchRequestId = new MediaPatchRequest();
        IdRequest<MediaPatchRequest, Integer> mediaPatchRequest = new IdRequest<>(mediaPatchRequestId, mediaRequestId);

        MediaRequestEntity entityResponse = new MediaRequestEntity();
        Mockito.when(mediaRequestRepository.findById(mediaRequestId)).thenReturn(Optional.of(entityResponse));

        audioMediaPatchRequestValidator.validate(mediaPatchRequest);
    }

    @Test
    void successfulPatchWithOwner() {
        Integer mediaRequestId = 200;
        Integer ownerIntger = 200;

        MediaPatchRequest mediaPatchRequestId = new MediaPatchRequest();
        mediaPatchRequestId.setOwnerId(ownerIntger);
        IdRequest<MediaPatchRequest, Integer> mediaPatchRequest = new IdRequest<>(mediaPatchRequestId, mediaRequestId);

        MediaRequestEntity entityResponse = new MediaRequestEntity();
        UserAccountEntity userAccountEntity = new UserAccountEntity();

        Mockito.when(mediaRequestRepository.findById(mediaRequestId)).thenReturn(Optional.of(entityResponse));
        Mockito.when(userAccountRepository.findById(ownerIntger)).thenReturn(Optional.of(userAccountEntity));

        audioMediaPatchRequestValidator.validate(mediaPatchRequest);
    }

    @Test
    void failureWithMediaRequestId() {
        Integer mediaRequestId = 200;
        Integer ownerIntger = 200;

        MediaPatchRequest mediaPatchRequestId = new MediaPatchRequest();
        mediaPatchRequestId.setOwnerId(ownerIntger);
        IdRequest<MediaPatchRequest, Integer> mediaPatchRequest = new IdRequest<>(mediaPatchRequestId, mediaRequestId);

        Mockito.when(mediaRequestRepository.findById(mediaRequestId)).thenReturn(Optional.empty());

        DartsApiException exception = Assertions.assertThrows(DartsApiException.class,
                                                              () -> audioMediaPatchRequestValidator.validate(mediaPatchRequest));
        Assertions.assertEquals(AudioRequestsApiError.MEDIA_REQUEST_NOT_FOUND, exception.getError());
    }

    @Test
    void failureWithUserOwnerId() {
        Integer mediaRequestId = 200;
        Integer ownerIntger = 200;

        MediaPatchRequest mediaPatchRequestId = new MediaPatchRequest();
        mediaPatchRequestId.setOwnerId(ownerIntger);
        IdRequest<MediaPatchRequest, Integer> mediaPatchRequest = new IdRequest<>(mediaPatchRequestId, mediaRequestId);

        MediaRequestEntity entityResponse = new MediaRequestEntity();

        Mockito.when(mediaRequestRepository.findById(mediaRequestId)).thenReturn(Optional.of(entityResponse));
        Mockito.when(userAccountRepository.findById(ownerIntger)).thenReturn(Optional.empty());

        DartsApiException exception = Assertions.assertThrows(DartsApiException.class,
                                                              () -> audioMediaPatchRequestValidator.validate(mediaPatchRequest));
        Assertions.assertEquals(AudioRequestsApiError.USER_IS_NOT_FOUND, exception.getError());
    }
}