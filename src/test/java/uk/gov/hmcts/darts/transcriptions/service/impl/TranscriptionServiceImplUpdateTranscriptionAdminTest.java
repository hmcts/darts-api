package uk.gov.hmcts.darts.transcriptions.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.TranscriptionCommentRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionStatusRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionWorkflowRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionAdminResponse;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionRequest;
import uk.gov.hmcts.darts.transcriptions.validator.WorkflowValidator;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;

@ExtendWith(MockitoExtension.class)
class TranscriptionServiceImplUpdateTranscriptionAdminTest {
    @Mock
    private TranscriptionRepository transcriptionRepository;
    @Mock
    private TranscriptionStatusRepository transcriptionStatusRepository;
    @Mock
    private TranscriptionWorkflowRepository transcriptionWorkflowRepository;
    @Mock
    private TranscriptionCommentRepository transcriptionCommentRepository;
    @Mock
    private WorkflowValidator workflowValidator;
    @Mock
    private AuditApi auditApi;
    @Mock
    private TranscriptionNotifications transcriptionNotifications;
    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private UserAccountEntity userAccountEntity;

    @Mock
    private UserIdentity mockUserIdentity;

    @InjectMocks
    private TranscriptionServiceImpl transcriptionService;

    private TranscriptionStatusEntity requestedTranscriptionStatus;
    private TranscriptionStatusEntity awaitingAuthorisationTranscriptionStatus;
    private TranscriptionTypeEntity transcriptionType;

    @BeforeEach
    void setUp() {
        var testUser = new UserAccountEntity();
        testUser.setEmailAddress("test.user@example.com");
        lenient().when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        requestedTranscriptionStatus = new TranscriptionStatusEntity();
        requestedTranscriptionStatus.setId(REQUESTED.getId());
        awaitingAuthorisationTranscriptionStatus = new TranscriptionStatusEntity();
        awaitingAuthorisationTranscriptionStatus.setId(AWAITING_AUTHORISATION.getId());

        transcriptionType = new TranscriptionTypeEntity();
    }

    @Test
    void updateTranscriptionAdmin_Success_ForManualTranscriptionAwaitingAuthToRequest() {
        Long transcriptionId = 1L;
        UpdateTranscriptionRequest updateRequest = new UpdateTranscriptionRequest();
        updateRequest.setTranscriptionStatusId(REQUESTED.getId());
        updateRequest.setWorkflowComment("Approved");

        TranscriptionEntity transcriptionEntity = new TranscriptionEntity();
        transcriptionEntity.setId(transcriptionId);
        transcriptionEntity.setIsManualTranscription(true);

        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.COURT_LOG;
        transcriptionType.setId(transcriptionTypeEnum.getId());
        transcriptionEntity.setTranscriptionType(transcriptionType);
        transcriptionEntity.setTranscriptionStatus(awaitingAuthorisationTranscriptionStatus);

        when(transcriptionRepository.findById(transcriptionId)).thenReturn(Optional.of(transcriptionEntity));
        when(transcriptionStatusRepository.getReferenceById(REQUESTED.getId()))
            .thenReturn(requestedTranscriptionStatus);
        when(transcriptionStatusRepository.getReferenceById(AWAITING_AUTHORISATION.getId()))
            .thenReturn(awaitingAuthorisationTranscriptionStatus);
        when(workflowValidator.validateChangeToWorkflowStatus(
            true, transcriptionTypeEnum, AWAITING_AUTHORISATION, REQUESTED, true)).thenReturn(true);
        doNothing().when(transcriptionNotifications).notifyApprovers(transcriptionEntity);

        TranscriptionCommentEntity transcriptionComment = mock(TranscriptionCommentEntity.class);
        when(transcriptionCommentRepository.saveAndFlush(any(TranscriptionCommentEntity.class))).thenReturn(transcriptionComment);

        UpdateTranscriptionAdminResponse response =
            transcriptionService.updateTranscriptionAdmin(transcriptionId, updateRequest, true);

        assertNotNull(response);
        assertEquals(transcriptionId, response.getTranscriptionId());
        assertEquals(AWAITING_AUTHORISATION.getId(), response.getTranscriptionStatusId());

        verify(transcriptionRepository).findById(transcriptionId);
        verify(transcriptionStatusRepository).getReferenceById(AWAITING_AUTHORISATION.getId());
        verify(transcriptionNotifications).notifyApprovers(transcriptionEntity);
    }

    @Test
    void updateTranscriptionAdmin_TranscriptionNotFound() {
        Long transcriptionId = 1L;
        UpdateTranscriptionRequest updateRequest = new UpdateTranscriptionRequest();
        updateRequest.setTranscriptionStatusId(2);

        when(transcriptionRepository.findById(transcriptionId)).thenReturn(Optional.empty());

        DartsApiException exception = assertThrows(DartsApiException.class, () ->
            transcriptionService.updateTranscriptionAdmin(transcriptionId, updateRequest, true));

        assertEquals("The requested transcription cannot be found", exception.getError().getTitle());

        verify(transcriptionRepository).findById(transcriptionId);
        verify(transcriptionStatusRepository, never()).getReferenceById(any());
        verify(transcriptionWorkflowRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateTranscriptionAdmin_InvalidWorkflowTransition() {
        Long transcriptionId = 1L;
        UpdateTranscriptionRequest updateRequest = new UpdateTranscriptionRequest();
        updateRequest.setTranscriptionStatusId(2);

        TranscriptionEntity transcriptionEntity = new TranscriptionEntity();
        transcriptionEntity.setId(transcriptionId);
        transcriptionEntity.setIsManualTranscription(false);

        TranscriptionTypeEntity transcriptionTypeEntity = new TranscriptionTypeEntity();
        transcriptionEntity.setTranscriptionType(transcriptionTypeEntity);

        when(transcriptionRepository.findById(transcriptionId)).thenReturn(Optional.of(transcriptionEntity));
        when(userAccountRepository.getReferenceById(any())).thenReturn(userAccountEntity);
        DartsApiException exception = assertThrows(DartsApiException.class, () ->
            transcriptionService.updateTranscriptionAdmin(transcriptionId, updateRequest, true));

        assertEquals("Unexpected transcription type for this workflow", exception.getError().getTitle());

        verify(transcriptionRepository).findById(transcriptionId);
        verify(transcriptionWorkflowRepository, never()).saveAndFlush(any());
    }
}
