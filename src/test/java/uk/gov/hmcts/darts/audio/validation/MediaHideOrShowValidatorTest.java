package uk.gov.hmcts.darts.audio.validation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.model.AdminActionRequest;
import uk.gov.hmcts.darts.audio.model.MediaHideRequest;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.ObjectHiddenReasonRepository;
import uk.gov.hmcts.darts.common.validation.IdRequest;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaHideOrShowValidatorTest {

    @Mock
    private MediaIdValidator mediaIdValidator;

    @Mock
    private ObjectHiddenReasonRepository objectHiddenReasonRepository;

    @Mock
    private ObjectAdminActionRepository objectAdminActionRepository;

    @InjectMocks
    private MediaHideOrShowValidator mediaHideOrShowValidator;

    private void setManualDeletionEnabled(boolean enabled) {
        this.mediaHideOrShowValidator = spy(mediaHideOrShowValidator);
        when(mediaHideOrShowValidator.isManualDeletionEnabled()).thenReturn(enabled);
    }

    @Test
    void successfullyShow() {
        Integer mediaId = 200;
        MediaHideRequest mediaHideRequest = new MediaHideRequest();
        mediaHideRequest.setIsHidden(false);

        IdRequest<MediaHideRequest> mediaHideRequestIdRequest = new
            IdRequest<>(mediaHideRequest, mediaId);

        mediaHideOrShowValidator.validate(mediaHideRequestIdRequest);

        Mockito.verify(mediaIdValidator, times(1)).validate(mediaId);
    }

    @Test
    void failShowWithAdminActionRequest() {
        Integer mediaId = 200;
        MediaHideRequest mediaHideRequest = new MediaHideRequest();
        mediaHideRequest.setIsHidden(false);
        mediaHideRequest.setAdminAction(new AdminActionRequest());

        IdRequest<MediaHideRequest> mediaHideRequestIdRequest = new
            IdRequest<>(mediaHideRequest, mediaId);

        DartsApiException exception = Assertions.assertThrows(DartsApiException.class,
                                                              () -> mediaHideOrShowValidator.validate(mediaHideRequestIdRequest));
        Assertions.assertEquals(AudioApiError.MEDIA_SHOW_ACTION_PAYLOAD_INCORRECT_USAGE, exception.getError());

        Mockito.verify(mediaIdValidator, times(1)).validate(mediaId);
    }

    @Test
    void successfullyHideWithActionRequest() {
        Integer mediaId = 200;
        MediaHideRequest mediaHideRequest = new MediaHideRequest();
        mediaHideRequest.setIsHidden(true);

        Integer reasonId = 949;
        AdminActionRequest request = new AdminActionRequest();
        mediaHideRequest.setAdminAction(request);
        request.setReasonId(reasonId);

        ObjectHiddenReasonEntity hiddenReasonEntity = new ObjectHiddenReasonEntity();

        when(objectHiddenReasonRepository.findById(reasonId)).thenReturn(Optional.of(hiddenReasonEntity));
        when(objectAdminActionRepository.findByMediaId(mediaId)).thenReturn(List.of());

        IdRequest<MediaHideRequest> mediaHideRequestIdRequest = new
            IdRequest<>(mediaHideRequest, mediaId);

        mediaHideOrShowValidator.validate(mediaHideRequestIdRequest);

        Mockito.verify(mediaIdValidator, times(1)).validate(mediaId);
    }

    @Test
    void failsWhenHideWithoutActionRequest() {
        Integer mediaId = 200;
        MediaHideRequest mediaHideRequest = new MediaHideRequest();
        mediaHideRequest.setIsHidden(true);

        IdRequest<MediaHideRequest> mediaHideRequestIdRequest = new
            IdRequest<>(mediaHideRequest, mediaId);

        DartsApiException exception =
            Assertions.assertThrows(DartsApiException.class, () -> mediaHideOrShowValidator.validate(mediaHideRequestIdRequest));
        Assertions.assertEquals(AudioApiError.MEDIA_HIDE_ACTION_PAYLOAD_INCORRECT_USAGE, exception.getError());

        Mockito.verify(mediaIdValidator, times(1)).validate(mediaId);
    }

    @Test
    void failWhenHideWithActionRequestWithDbAction() {
        Integer mediaId = 200;
        AdminActionRequest adminActionResponse = new AdminActionRequest();

        MediaHideRequest mediaHideRequest = new MediaHideRequest();
        mediaHideRequest.setIsHidden(true);
        mediaHideRequest.setAdminAction(adminActionResponse);

        ObjectAdminActionEntity objectAdminActionEntity = new ObjectAdminActionEntity();

        when(objectAdminActionRepository.findByMediaId(mediaId)).thenReturn(List.of(objectAdminActionEntity));

        IdRequest<MediaHideRequest> mediaHideRequestIdRequest = new
            IdRequest<>(mediaHideRequest, mediaId);

        DartsApiException exception =
            Assertions.assertThrows(DartsApiException.class, () -> mediaHideOrShowValidator.validate(mediaHideRequestIdRequest));
        Assertions.assertEquals(AudioApiError.MEDIA_ALREADY_HIDDEN, exception.getError());

        Mockito.verify(mediaIdValidator, times(1)).validate(mediaId);
    }

    @Test
    void failWhenHideWithActionRequestAndWithoutCorrectReason() {
        Integer mediaId = 200;
        AdminActionRequest adminActionResponse = new AdminActionRequest();

        MediaHideRequest mediaHideRequest = new MediaHideRequest();
        mediaHideRequest.setIsHidden(true);
        mediaHideRequest.setAdminAction(adminActionResponse);

        Integer reasonId = 949;
        adminActionResponse.setReasonId(reasonId);

        when(objectHiddenReasonRepository.findById(reasonId)).thenReturn(Optional.empty());
        when(objectAdminActionRepository.findByMediaId(mediaId)).thenReturn(List.of());

        IdRequest<MediaHideRequest> mediaHideRequestIdRequest = new
            IdRequest<>(mediaHideRequest, mediaId);

        DartsApiException exception
            = Assertions.assertThrows(DartsApiException.class, () -> mediaHideOrShowValidator.validate(mediaHideRequestIdRequest));
        Assertions.assertEquals(AudioApiError.MEDIA_HIDE_ACTION_REASON_NOT_FOUND, exception.getError());

        Mockito.verify(mediaIdValidator, times(1)).validate(mediaId);
    }


    @Test
    void successfullyHideWithMarkedForDeletionActionRequestAndFlagEnabled() {
        setManualDeletionEnabled(true);
        Integer mediaId = 200;
        MediaHideRequest mediaHideRequest = new MediaHideRequest();
        mediaHideRequest.setIsHidden(true);

        Integer reasonId = 949;
        AdminActionRequest request = new AdminActionRequest();
        mediaHideRequest.setAdminAction(request);
        request.setReasonId(reasonId);

        ObjectHiddenReasonEntity hiddenReasonEntity = new ObjectHiddenReasonEntity();
        hiddenReasonEntity.setMarkedForDeletion(true);

        when(objectHiddenReasonRepository.findById(reasonId)).thenReturn(Optional.of(hiddenReasonEntity));
        when(objectAdminActionRepository.findByMediaId(mediaId)).thenReturn(List.of());

        IdRequest<MediaHideRequest> mediaHideRequestIdRequest = new
            IdRequest<>(mediaHideRequest, mediaId);

        mediaHideOrShowValidator.validate(mediaHideRequestIdRequest);

        Mockito.verify(mediaIdValidator, times(1)).validate(mediaId);
    }

    @Test
    void unsuccessfullyHideWithMarkedForDeletionActionRequestAndFlagDisabled() {
        setManualDeletionEnabled(false);
        Integer mediaId = 200;
        MediaHideRequest mediaHideRequest = new MediaHideRequest();
        mediaHideRequest.setIsHidden(true);

        Integer reasonId = 949;
        AdminActionRequest request = new AdminActionRequest();
        mediaHideRequest.setAdminAction(request);
        request.setReasonId(reasonId);

        ObjectHiddenReasonEntity hiddenReasonEntity = new ObjectHiddenReasonEntity();
        hiddenReasonEntity.setMarkedForDeletion(true);

        when(objectHiddenReasonRepository.findById(reasonId)).thenReturn(Optional.of(hiddenReasonEntity));
        when(objectAdminActionRepository.findByMediaId(mediaId)).thenReturn(List.of());

        IdRequest<MediaHideRequest> mediaHideRequestIdRequest = new
            IdRequest<>(mediaHideRequest, mediaId);

        DartsApiException exception
            = Assertions.assertThrows(DartsApiException.class, () -> mediaHideOrShowValidator.validate(mediaHideRequestIdRequest));
        Assertions.assertEquals(CommonApiError.FEATURE_FLAG_NOT_ENABLED, exception.getError());

        Mockito.verify(mediaIdValidator, times(1)).validate(mediaId);
    }

}