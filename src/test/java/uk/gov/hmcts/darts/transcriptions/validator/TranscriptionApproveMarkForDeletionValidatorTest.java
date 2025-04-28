package uk.gov.hmcts.darts.transcriptions.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.ObjectHiddenReasonRepository;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranscriptionApproveMarkForDeletionValidatorTest {

    @Mock
    private ObjectAdminActionRepository objectAdminActionRepository;
    @Mock
    private ObjectHiddenReasonRepository objectHiddenReasonRepository;
    @Mock
    private UserIdentity userIdentity;
    @Mock
    private TranscriptionDocumentIdValidator transcriptionDocumentIdValidator;

    @InjectMocks
    private TranscriptionApproveMarkForDeletionValidator validator;

    private static final Long TRANSCRIPTION_DOCUMENT_ID = 1L;

    @BeforeEach
    void setUp() {
        doNothing().when(transcriptionDocumentIdValidator).validate(TRANSCRIPTION_DOCUMENT_ID);
    }

    @Test
    void validate_shouldThrowException_whenNoObjectAdminActionFound() {
        when(objectAdminActionRepository.findByTranscriptionDocumentId(TRANSCRIPTION_DOCUMENT_ID))
            .thenReturn(Collections.emptyList());

        DartsApiException exception = assertThrows(DartsApiException.class,
                                                   () -> validator.validate(TRANSCRIPTION_DOCUMENT_ID));
        assertEquals(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_ID_NOT_FOUND, exception.getError());
    }

    @Test
    void validate_shouldThrowException_whenMultipleObjectAdminActionsFound() {
        when(objectAdminActionRepository.findByTranscriptionDocumentId(TRANSCRIPTION_DOCUMENT_ID))
            .thenReturn(List.of(new ObjectAdminActionEntity(), new ObjectAdminActionEntity()));

        DartsApiException exception = assertThrows(DartsApiException.class,
                                                   () -> validator.validate(TRANSCRIPTION_DOCUMENT_ID));
        assertEquals(TranscriptionApiError.TOO_MANY_RESULTS, exception.getError());
    }

    @Test
    void validate_shouldThrowException_whenAlreadyMarkedForManualDeletion() {
        ObjectAdminActionEntity entity = new ObjectAdminActionEntity();
        entity.setMarkedForManualDeletion(true);
        when(objectAdminActionRepository.findByTranscriptionDocumentId(TRANSCRIPTION_DOCUMENT_ID))
            .thenReturn(List.of(entity));

        DartsApiException exception = assertThrows(DartsApiException.class,
                                                   () -> validator.validate(TRANSCRIPTION_DOCUMENT_ID));
        assertEquals(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_DELETION_ALREADY_APPROVED, exception.getError());
    }

    @Test
    void validate_shouldThrowException_whenHiddenReasonNotFound() {
        ObjectAdminActionEntity entity = new ObjectAdminActionEntity();
        entity.setObjectHiddenReason(new ObjectHiddenReasonEntity());
        when(objectAdminActionRepository.findByTranscriptionDocumentId(TRANSCRIPTION_DOCUMENT_ID))
            .thenReturn(List.of(entity));
        when(objectHiddenReasonRepository.findById(any())).thenReturn(Optional.empty());

        DartsApiException exception = assertThrows(DartsApiException.class,
                                                   () -> validator.validate(TRANSCRIPTION_DOCUMENT_ID));
        assertEquals(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_MARKED_FOR_DELETION_REASON_NOT_FOUND, exception.getError());
    }

    @Test
    void validate_shouldThrowException_whenHiddenReasonNotMarkedForDeletion() {
        ObjectAdminActionEntity entity = new ObjectAdminActionEntity();
        ObjectHiddenReasonEntity hiddenReason = new ObjectHiddenReasonEntity();
        hiddenReason.setMarkedForDeletion(false);
        entity.setObjectHiddenReason(hiddenReason);
        when(objectAdminActionRepository.findByTranscriptionDocumentId(TRANSCRIPTION_DOCUMENT_ID))
            .thenReturn(List.of(entity));
        when(objectHiddenReasonRepository.findById(any())).thenReturn(Optional.of(hiddenReason));

        DartsApiException exception = assertThrows(DartsApiException.class,
                                                   () -> validator.validate(TRANSCRIPTION_DOCUMENT_ID));
        assertEquals(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_MARKED_FOR_DELETION_REASON_NOT_FOUND, exception.getError());
    }

    @Test
    void validate_shouldThrowException_whenApprovedByHiddenByUser() {
        ObjectAdminActionEntity entity = new ObjectAdminActionEntity();
        ObjectHiddenReasonEntity hiddenReason = new ObjectHiddenReasonEntity();
        hiddenReason.setMarkedForDeletion(true);
        entity.setObjectHiddenReason(hiddenReason);
        UserAccountEntity user = new UserAccountEntity();
        user.setId(1);
        entity.setHiddenBy(user);
        when(objectAdminActionRepository.findByTranscriptionDocumentId(TRANSCRIPTION_DOCUMENT_ID))
            .thenReturn(List.of(entity));
        when(objectHiddenReasonRepository.findById(any())).thenReturn(Optional.of(hiddenReason));
        when(userIdentity.getUserAccount()).thenReturn(user);

        DartsApiException exception = assertThrows(DartsApiException.class,
                                                   () -> validator.validate(TRANSCRIPTION_DOCUMENT_ID));
        assertEquals(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_DELETION_CAN_NOT_APPROVE_OWN_REQUEST, exception.getError());
    }

    @Test
    void validate_shouldNotThrowException_whenAllConditionsAreMet() {
        ObjectAdminActionEntity entity = new ObjectAdminActionEntity();
        ObjectHiddenReasonEntity hiddenReason = new ObjectHiddenReasonEntity();
        hiddenReason.setMarkedForDeletion(true);
        entity.setObjectHiddenReason(hiddenReason);
        UserAccountEntity hiddenByUser = new UserAccountEntity();
        hiddenByUser.setId(1);
        entity.setHiddenBy(hiddenByUser);
        UserAccountEntity currentUser = new UserAccountEntity();
        currentUser.setId(2);
        when(objectAdminActionRepository.findByTranscriptionDocumentId(TRANSCRIPTION_DOCUMENT_ID))
            .thenReturn(List.of(entity));
        when(objectHiddenReasonRepository.findById(any())).thenReturn(Optional.of(hiddenReason));
        when(userIdentity.getUserAccount()).thenReturn(currentUser);

        assertDoesNotThrow(() -> validator.validate(TRANSCRIPTION_DOCUMENT_ID));

        verify(objectAdminActionRepository, times(1))
            .findByTranscriptionDocumentId(TRANSCRIPTION_DOCUMENT_ID);
        verify(objectHiddenReasonRepository, times(1))
            .findById(any());
        verify(userIdentity, times(1))
            .getUserAccount();
        verifyNoMoreInteractions(objectAdminActionRepository, objectHiddenReasonRepository, userIdentity);
    }

    @Test
    void validate_shouldNotThrowException_whenObjectHiddenReasonIsNull() {
        ObjectAdminActionEntity entity = new ObjectAdminActionEntity();
        entity.setObjectHiddenReason(null);
        when(objectAdminActionRepository.findByTranscriptionDocumentId(TRANSCRIPTION_DOCUMENT_ID))
            .thenReturn(List.of(entity));

        assertDoesNotThrow(() -> validator.validate(TRANSCRIPTION_DOCUMENT_ID));
    }
}