package uk.gov.hmcts.darts.transcriptions.validator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.ObjectHiddenReasonRepository;
import uk.gov.hmcts.darts.common.validation.IdRequest;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.model.AdminActionRequest;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentHideRequest;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranscriptionDocumentHideOrShowValidatorTest {

    @Mock
    private TranscriptionDocumentIdValidator transcriptionDocumentIdValidator;

    @Mock
    private ObjectHiddenReasonRepository objectHiddenReasonRepository;

    @Mock
    private ObjectAdminActionRepository objectAdminActionRepository;

    @InjectMocks
    private TranscriptionDocumentHideOrShowValidator transcriptionDocumentHideOrShowValidator;


    private void setManualDeletionEnabled(boolean enabled) {
        this.transcriptionDocumentHideOrShowValidator = spy(transcriptionDocumentHideOrShowValidator);
        when(transcriptionDocumentHideOrShowValidator.isManualDeletionEnabled()).thenReturn(enabled);
    }

    @Test
    void successfullyShow() {
        Long documentId = 200L;
        TranscriptionDocumentHideRequest transcriptionDocumentHideRequest = new TranscriptionDocumentHideRequest();
        transcriptionDocumentHideRequest.setIsHidden(false);

        IdRequest<TranscriptionDocumentHideRequest, Long> transcriptionDocumentEntityUserId = new
            IdRequest<>(transcriptionDocumentHideRequest, documentId);

        transcriptionDocumentHideOrShowValidator.validate(transcriptionDocumentEntityUserId);

        Mockito.verify(transcriptionDocumentIdValidator, times(1)).validate(documentId);
    }

    @Test
    void failShowWithAdminActionRequest() {
        Long documentId = 200L;
        TranscriptionDocumentHideRequest transcriptionDocumentHideRequest = new TranscriptionDocumentHideRequest();
        transcriptionDocumentHideRequest.setIsHidden(false);

        transcriptionDocumentHideRequest.setAdminAction(new AdminActionRequest());

        IdRequest<TranscriptionDocumentHideRequest, Long> transcriptionDocumentEntityUserId = new
            IdRequest<>(transcriptionDocumentHideRequest, documentId);

        DartsApiException exception = Assertions.assertThrows(DartsApiException.class,
                                                              () -> transcriptionDocumentHideOrShowValidator.validate(transcriptionDocumentEntityUserId));
        Assertions.assertEquals(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_SHOW_ACTION_PAYLOAD_INCORRECT_USAGE, exception.getError());

        Mockito.verify(transcriptionDocumentIdValidator, times(1)).validate(documentId);
    }

    @Test
    void successfullyHideWithActionRequest() {
        Long documentId = 200L;
        TranscriptionDocumentHideRequest transcriptionDocumentHideRequest = new TranscriptionDocumentHideRequest();
        transcriptionDocumentHideRequest.setIsHidden(true);

        Integer reasonId = 949;
        AdminActionRequest request = new AdminActionRequest();
        transcriptionDocumentHideRequest.setAdminAction(request);
        request.setReasonId(reasonId);

        ObjectHiddenReasonEntity hiddenReasonEntity = new ObjectHiddenReasonEntity();

        when(objectHiddenReasonRepository.findById(reasonId)).thenReturn(Optional.of(hiddenReasonEntity));
        when(objectAdminActionRepository.findByTranscriptionDocumentId(documentId)).thenReturn(List.of());

        IdRequest<TranscriptionDocumentHideRequest, Long> transcriptionDocumentEntityUserId = new
            IdRequest<>(transcriptionDocumentHideRequest, documentId);

        transcriptionDocumentHideOrShowValidator.validate(transcriptionDocumentEntityUserId);

        Mockito.verify(transcriptionDocumentIdValidator, times(1)).validate(documentId);
    }

    @Test
    void failsWhenHideWithoutActionRequest() {
        Long documentId = 200L;
        TranscriptionDocumentHideRequest transcriptionDocumentHideRequest = new TranscriptionDocumentHideRequest();
        transcriptionDocumentHideRequest.setIsHidden(true);

        IdRequest<TranscriptionDocumentHideRequest, Long> transcriptionDocumentEntityUserId = new
            IdRequest<>(transcriptionDocumentHideRequest, documentId);

        DartsApiException exception =
            Assertions.assertThrows(DartsApiException.class, () -> transcriptionDocumentHideOrShowValidator.validate(transcriptionDocumentEntityUserId));
        Assertions.assertEquals(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_HIDE_ACTION_PAYLOAD_INCORRECT_USAGE, exception.getError());

        Mockito.verify(transcriptionDocumentIdValidator, times(1)).validate(documentId
        );
    }

    @Test
    void failWhenHideWithActionRequestWithDbAction() {
        Long documentId = 200L;
        AdminActionRequest adminActionResponse = new AdminActionRequest();

        TranscriptionDocumentHideRequest transcriptionDocumentHideRequest = new TranscriptionDocumentHideRequest();
        transcriptionDocumentHideRequest.setIsHidden(true);
        transcriptionDocumentHideRequest.setAdminAction(adminActionResponse);

        ObjectAdminActionEntity objectAdminActionEntity = new ObjectAdminActionEntity();

        when(objectAdminActionRepository.findByTranscriptionDocumentId(documentId)).thenReturn(List.of(objectAdminActionEntity));

        IdRequest<TranscriptionDocumentHideRequest, Long> transcriptionDocumentEntityUserId = new
            IdRequest<>(transcriptionDocumentHideRequest, documentId);

        DartsApiException exception =
            Assertions.assertThrows(DartsApiException.class, () -> transcriptionDocumentHideOrShowValidator.validate(transcriptionDocumentEntityUserId));
        Assertions.assertEquals(TranscriptionApiError.TRANSCRIPTION_ALREADY_HIDDEN, exception.getError());

        Mockito.verify(transcriptionDocumentIdValidator, times(1)).validate(documentId);
    }

    @Test
    void failWhenHideWithActionRequestAndWithoutCorrectReason() {
        Long documentId = 200L;
        AdminActionRequest adminActionResponse = new AdminActionRequest();

        TranscriptionDocumentHideRequest transcriptionDocumentHideRequest = new TranscriptionDocumentHideRequest();
        transcriptionDocumentHideRequest.setIsHidden(true);
        transcriptionDocumentHideRequest.setAdminAction(adminActionResponse);

        Integer reasonId = 949;
        adminActionResponse.setReasonId(reasonId);

        when(objectHiddenReasonRepository.findById(reasonId)).thenReturn(Optional.empty());
        when(objectAdminActionRepository.findByTranscriptionDocumentId(documentId)).thenReturn(List.of());

        IdRequest<TranscriptionDocumentHideRequest, Long> transcriptionDocumentEntityUserId = new
            IdRequest<>(transcriptionDocumentHideRequest, documentId);

        DartsApiException exception
            = Assertions.assertThrows(DartsApiException.class, () -> transcriptionDocumentHideOrShowValidator.validate(transcriptionDocumentEntityUserId));
        Assertions.assertEquals(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_HIDE_ACTION_REASON_NOT_FOUND, exception.getError());

        Mockito.verify(transcriptionDocumentIdValidator, times(1)).validate(documentId);
    }

    @Test
    void failWhenHideWithActionRequestAndWithReasonMarkedForDeletionButManualDeletionIsDisabled() {
        setManualDeletionEnabled(false);
        Long documentId = 200L;
        AdminActionRequest adminActionResponse = new AdminActionRequest();

        TranscriptionDocumentHideRequest transcriptionDocumentHideRequest = new TranscriptionDocumentHideRequest();
        transcriptionDocumentHideRequest.setIsHidden(true);
        transcriptionDocumentHideRequest.setAdminAction(adminActionResponse);

        Integer reasonId = 949;
        adminActionResponse.setReasonId(reasonId);

        ObjectHiddenReasonEntity reasonEntity = Mockito.mock(ObjectHiddenReasonEntity.class);
        when(reasonEntity.isMarkedForDeletion()).thenReturn(true);

        when(objectHiddenReasonRepository.findById(reasonId)).thenReturn(Optional.of(reasonEntity));
        when(objectAdminActionRepository.findByTranscriptionDocumentId(documentId)).thenReturn(List.of());

        IdRequest<TranscriptionDocumentHideRequest, Long> transcriptionDocumentEntityUserId = new
            IdRequest<>(transcriptionDocumentHideRequest, documentId);

        DartsApiException exception
            = Assertions.assertThrows(DartsApiException.class, () -> transcriptionDocumentHideOrShowValidator.validate(transcriptionDocumentEntityUserId));
        Assertions.assertEquals(CommonApiError.FEATURE_FLAG_NOT_ENABLED, exception.getError());

        Mockito.verify(transcriptionDocumentIdValidator, times(1)).validate(documentId);
    }

    @Test
    void successfullyHideWithActionRequestAndWithReasonMarkedForDeletionButManualDeletionIsEnabled() {
        setManualDeletionEnabled(true);
        Long documentId = 200L;
        AdminActionRequest adminActionResponse = new AdminActionRequest();

        TranscriptionDocumentHideRequest transcriptionDocumentHideRequest = new TranscriptionDocumentHideRequest();
        transcriptionDocumentHideRequest.setIsHidden(true);
        transcriptionDocumentHideRequest.setAdminAction(adminActionResponse);

        Integer reasonId = 949;
        adminActionResponse.setReasonId(reasonId);

        ObjectHiddenReasonEntity reasonEntity = Mockito.mock(ObjectHiddenReasonEntity.class);
        when(reasonEntity.isMarkedForDeletion()).thenReturn(true);

        when(objectHiddenReasonRepository.findById(reasonId)).thenReturn(Optional.of(reasonEntity));
        when(objectAdminActionRepository.findByTranscriptionDocumentId(documentId)).thenReturn(List.of());

        IdRequest<TranscriptionDocumentHideRequest, Long> transcriptionDocumentEntityUserId = new
            IdRequest<>(transcriptionDocumentHideRequest, documentId);

        Assertions.assertDoesNotThrow(() -> transcriptionDocumentHideOrShowValidator.validate(transcriptionDocumentEntityUserId));
    }
}