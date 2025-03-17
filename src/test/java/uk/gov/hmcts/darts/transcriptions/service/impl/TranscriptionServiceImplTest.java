package uk.gov.hmcts.darts.transcriptions.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.TranscriptionCommentRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionStatusRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionTypeRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionUrgencyRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionWorkflowRepository;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.common.util.TranscriptionUrgencyEnum;
import uk.gov.hmcts.darts.hearings.service.HearingsService;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.mapper.TranscriptionResponseMapper;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionByIdResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.REQUEST_TRANSCRIPTION;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;
import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.TRANSCRIPTION_NOT_FOUND;

@Slf4j
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.LawOfDemeter", "PMD.ExcessiveImports"})
class TranscriptionServiceImplTest {

    private static final String TEST_COMMENT = "Test comment";
    private static final String START_TIME = "2023-07-01T09:00:00";
    private static final String END_TIME = "2023-07-01T12:00:00";
    private static final int CASE_ID = 33;

    @Mock
    private TranscriptionRepository mockTranscriptionRepository;
    @Mock
    private TranscriptionDocumentRepository mockTranscriptionDocumentRepository;
    @Mock
    private TranscriptionStatusRepository mockTranscriptionStatusRepository;
    @Mock
    private TranscriptionTypeRepository mockTranscriptionTypeRepository;
    @Mock
    private TranscriptionUrgencyRepository mockTranscriptionUrgencyRepository;
    @Mock
    private TranscriptionWorkflowRepository mockTranscriptionWorkflowRepository;

    @Mock
    private TranscriptionCommentRepository mockTranscriptionCommentRepository;

    @Mock
    private TranscriptionWorkflowEntity mockTranscriptionWorkflowEntity;

    @Mock
    private CaseService mockCaseService;
    @Mock
    private HearingsService mockHearingsService;
    @Mock
    private AuditApi mockAuditApi;

    @Mock
    private UserIdentity mockUserIdentity;

    @Mock
    private AuthorisationApi authorisationApi;

    @Mock
    private TranscriptionNotifications transcriptionNotifications;

    @Mock
    private TranscriptionLinkedCaseRepository transcriptionLinkedCaseRepository;

    private HearingEntity mockHearing;
    private CourtCaseEntity mockCourtCase;
    private TranscriptionUrgencyEntity mockTranscriptionUrgency;
    private TranscriptionTypeEntity mockTranscriptionType;
    private TranscriptionStatusEntity requestedTranscriptionStatus;
    private TranscriptionStatusEntity awaitingAuthorisationTranscriptionStatus;
    private UserAccountEntity testUser;

    @Mock
    private TranscriptionEntity mockTranscription;
    @Mock
    private TranscriptionEntity mockTranscription2;

    @Mock
    private DuplicateRequestDetector duplicateRequestDetector;

    @Mock
    private TranscriptionResponseMapper transcriptionResponseMapper;

    @Captor
    private ArgumentCaptor<TranscriptionEntity> transcriptionEntityArgumentCaptor;

    @Captor
    private ArgumentCaptor<TranscriptionWorkflowEntity> transcriptionWorkflowEntityArgumentCaptor;

    @Captor
    private ArgumentCaptor<TranscriptionCommentEntity> transcriptionCommentEntityArgumentCaptor;

    @Captor
    private ArgumentCaptor<TranscriptionLinkedCaseEntity> transcriptionLinkedCaseEntityArgumentCaptor;

    @InjectMocks
    private TranscriptionServiceImpl transcriptionService;

    @BeforeEach
    void setUp() {

        testUser = new UserAccountEntity();
        testUser.setEmailAddress("test.user@example.com");
        lenient().when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        mockCourtCase = new CourtCaseEntity();
        mockHearing = new HearingEntity();
        mockHearing.setCourtroom(new CourtroomEntity());
        mockHearing.setCourtCase(mockCourtCase);

        mockTranscriptionUrgency = new TranscriptionUrgencyEntity();
        mockTranscriptionType = new TranscriptionTypeEntity();
        requestedTranscriptionStatus = new TranscriptionStatusEntity();
        requestedTranscriptionStatus.setId(REQUESTED.getId());
        awaitingAuthorisationTranscriptionStatus = new TranscriptionStatusEntity();
        awaitingAuthorisationTranscriptionStatus.setId(AWAITING_AUTHORISATION.getId());

        List<UserAccountEntity> approvers = new ArrayList<>();
        UserAccountEntity approver1 = new UserAccountEntity();
        UserAccountEntity approver2 = new UserAccountEntity();
        approver1.setEmailAddress("approver1@example.com");
        approver2.setEmailAddress("approver2@example.com");
        approvers.add(approver1);
        approvers.add(approver2);

        lenient().when(authorisationApi.getUsersWithRoleAtCourthouse(eq(SecurityRoleEnum.APPROVER), any())).thenReturn(approvers);
        lenient().when(mockTranscriptionDocumentRepository.findByTranscriptionIdAndHiddenTrueIncludeDeleted(anyInt()))
            .thenReturn(Collections.emptyList());
    }

    private void updateManualDeletion(boolean manualDeletionEnabled) {
        this.transcriptionService = spy(transcriptionService);
        when(transcriptionService.isManualDeletionEnabled()).thenReturn(manualDeletionEnabled);
    }


    @Test
    void saveTranscriptionRequestWithValidValuesAndCourtLogTypeReturnSuccess() {

        doNothing().when(duplicateRequestDetector).checkForDuplicate(any(TranscriptionRequestDetails.class), any(Boolean.class));

        Integer hearingId = 1;
        when(mockHearingsService.getHearingByIdWithValidation(hearingId)).thenReturn(mockHearing);

        Integer caseId = 1;
        when(mockCaseService.getCourtCaseById(caseId)).thenReturn(mockCourtCase);

        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.OVERNIGHT;
        when(mockTranscriptionUrgencyRepository.getReferenceById(transcriptionUrgencyEnum.getId()))
            .thenReturn(mockTranscriptionUrgency);

        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.COURT_LOG;
        when(mockTranscriptionTypeRepository.getReferenceById(transcriptionTypeEnum.getId()))
            .thenReturn(mockTranscriptionType);

        when(mockTranscriptionStatusRepository.getReferenceById(REQUESTED.getId()))
            .thenReturn(requestedTranscriptionStatus);
        when(mockTranscriptionStatusRepository.getReferenceById(AWAITING_AUTHORISATION.getId()))
            .thenReturn(awaitingAuthorisationTranscriptionStatus);

        when(mockTranscriptionRepository.saveAndFlush(any(TranscriptionEntity.class))).thenReturn(mockTranscription);
        when(mockTranscriptionWorkflowRepository.saveAndFlush(any())).thenReturn(mockTranscriptionWorkflowEntity);

        mockTranscriptionType.setId(transcriptionTypeEnum.getId());
        when(mockTranscription.getIsManualTranscription()).thenReturn(true);

        when(mockTranscription.getCourtCase()).thenReturn(mockCourtCase);
        doNothing().when(mockAuditApi).record(REQUEST_TRANSCRIPTION, testUser, mockCourtCase);

        OffsetDateTime startDateTime = CommonTestDataUtil.createOffsetDateTime(START_TIME);
        OffsetDateTime endDateTime = CommonTestDataUtil.createOffsetDateTime(END_TIME);

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            hearingId,
            caseId,
            transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(),
            TEST_COMMENT,
            startDateTime,
            endDateTime);

        transcriptionService.saveTranscriptionRequest(transcriptionRequestDetails, true);

        verify(duplicateRequestDetector).checkForDuplicate(transcriptionRequestDetails, true);
        verify(mockTranscriptionRepository).saveAndFlush(transcriptionEntityArgumentCaptor.capture());

        TranscriptionEntity transcriptionEntity = transcriptionEntityArgumentCaptor.getValue();
        assertThat(transcriptionEntity.getHearing()).isNotNull();
        assertThat(transcriptionEntity.getCourtCase()).isNotNull();
        assertThat(transcriptionEntity.getCourtroom()).isEqualTo(mockHearing.getCourtroom());
        assertThat(transcriptionEntity.getHearingDate()).isNull();
        assertThat(transcriptionEntity.getTranscriptionStatus()).isEqualTo(requestedTranscriptionStatus);
        assertThat(transcriptionEntity.getTranscriptionType()).isEqualTo(mockTranscriptionType);
        assertThat(transcriptionEntity.getTranscriptionUrgency()).isEqualTo(mockTranscriptionUrgency);
        assertThat(transcriptionEntity.getStartTime()).isEqualTo(startDateTime);
        assertThat(transcriptionEntity.getEndTime()).isEqualTo(endDateTime);
        assertThat(transcriptionEntity.getIsManualTranscription()).isTrue();
        assertThat(transcriptionEntity.getIsCurrent()).isTrue();
        assertThat(transcriptionEntity.getRequestedBy()).isEqualTo(testUser);

        verify(
            mockTranscriptionWorkflowRepository,
            times(2)
        ).saveAndFlush(transcriptionWorkflowEntityArgumentCaptor.capture());

        verify(
            mockTranscriptionCommentRepository,
            times(1)
        ).saveAndFlush(transcriptionCommentEntityArgumentCaptor.capture());

        List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = transcriptionWorkflowEntityArgumentCaptor.getAllValues();
        TranscriptionWorkflowEntity requestedTranscriptionWorkflowEntity = transcriptionWorkflowEntities.getFirst();
        assertThat(requestedTranscriptionWorkflowEntity.getTranscriptionStatus().getId()).isEqualTo(REQUESTED.getId());
        TranscriptionWorkflowEntity awaitingAuthorisationTranscriptionWorkflowEntity = transcriptionWorkflowEntities.get(
            1);
        assertThat(awaitingAuthorisationTranscriptionWorkflowEntity.getTranscriptionStatus().getId()).isEqualTo(
            AWAITING_AUTHORISATION.getId());

        verifyTranscriptionLinkedCaseEntity(mockTranscription, mockCourtCase);

        assertTranscriptionComments();
        verifyNotification();

        verify(mockAuditApi).record(REQUEST_TRANSCRIPTION, testUser, mockCourtCase);
    }


    @Test
    void saveTranscriptionRequestWithValidValuesNullHearingAndCourtLogTypeReturnSuccess() {
        doNothing().when(duplicateRequestDetector).checkForDuplicate(any(TranscriptionRequestDetails.class), any(Boolean.class));

        Integer caseId = 1;
        when(mockCaseService.getCourtCaseById(caseId)).thenReturn(mockCourtCase);

        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.OVERNIGHT;
        when(mockTranscriptionUrgencyRepository.getReferenceById(transcriptionUrgencyEnum.getId()))
            .thenReturn(mockTranscriptionUrgency);

        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.COURT_LOG;
        when(mockTranscriptionTypeRepository.getReferenceById(transcriptionTypeEnum.getId()))
            .thenReturn(mockTranscriptionType);

        when(mockTranscriptionStatusRepository.getReferenceById(REQUESTED.getId()))
            .thenReturn(requestedTranscriptionStatus);
        when(mockTranscriptionStatusRepository.getReferenceById(AWAITING_AUTHORISATION.getId()))
            .thenReturn(awaitingAuthorisationTranscriptionStatus);

        when(mockTranscriptionRepository.saveAndFlush(any(TranscriptionEntity.class))).thenReturn(mockTranscription);
        when(mockTranscriptionWorkflowRepository.saveAndFlush(any())).thenReturn(mockTranscriptionWorkflowEntity);

        mockTranscriptionType.setId(transcriptionTypeEnum.getId());
        when(mockTranscription.getIsManualTranscription()).thenReturn(true);

        when(mockTranscription.getCourtCase()).thenReturn(mockCourtCase);
        doNothing().when(mockAuditApi).record(REQUEST_TRANSCRIPTION, testUser, mockCourtCase);

        Integer hearingId = null;
        OffsetDateTime startDateTime = CommonTestDataUtil.createOffsetDateTime(START_TIME);
        OffsetDateTime endDateTime = CommonTestDataUtil.createOffsetDateTime(END_TIME);

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            hearingId,
            caseId,
            transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(),
            TEST_COMMENT,
            startDateTime,
            endDateTime);

        transcriptionService.saveTranscriptionRequest(transcriptionRequestDetails, true);

        verify(duplicateRequestDetector).checkForDuplicate(transcriptionRequestDetails, true);
        verify(mockTranscriptionRepository).saveAndFlush(transcriptionEntityArgumentCaptor.capture());

        TranscriptionEntity transcriptionEntity = transcriptionEntityArgumentCaptor.getValue();
        assertThat(transcriptionEntity.getHearing()).isNull();
        assertThat(transcriptionEntity.getCourtCase()).isNotNull();
        assertThat(transcriptionEntity.getCourtroom()).isNull();
        assertThat(transcriptionEntity.getTranscriptionUrgency()).isNotNull();
        assertThat(transcriptionEntity.getStartTime()).isEqualTo(startDateTime);
        assertThat(transcriptionEntity.getEndTime()).isEqualTo(endDateTime);
        assertThat(transcriptionEntity.getIsManualTranscription()).isTrue();
        assertThat(transcriptionEntity.getIsCurrent()).isTrue();

        verify(
            mockTranscriptionWorkflowRepository,
            times(2)
        ).saveAndFlush(transcriptionWorkflowEntityArgumentCaptor.capture());

        verify(
            mockTranscriptionCommentRepository,
            times(1)
        ).saveAndFlush(transcriptionCommentEntityArgumentCaptor.capture());

        List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = transcriptionWorkflowEntityArgumentCaptor.getAllValues();
        TranscriptionWorkflowEntity requestedTranscriptionWorkflowEntity = transcriptionWorkflowEntities.getFirst();
        assertThat(requestedTranscriptionWorkflowEntity.getTranscriptionStatus().getId()).isEqualTo(REQUESTED.getId());
        TranscriptionWorkflowEntity awaitingAuthorisationTranscriptionWorkflowEntity = transcriptionWorkflowEntities.get(
            1);
        assertThat(awaitingAuthorisationTranscriptionWorkflowEntity.getTranscriptionStatus().getId()).isEqualTo(
            AWAITING_AUTHORISATION.getId());

        verifyTranscriptionLinkedCaseEntity(mockTranscription, mockCourtCase);
        assertTranscriptionComments();
        verifyNotification();

        verify(mockAuditApi).record(REQUEST_TRANSCRIPTION, testUser, mockCourtCase);
    }

    @Test
    void saveTranscriptionRequestWithValidValuesNullCourtCaseAndCourtLogTypeReturnSuccess() {
        doNothing().when(duplicateRequestDetector).checkForDuplicate(any(TranscriptionRequestDetails.class), any(Boolean.class));

        Integer hearingId = 1;
        when(mockHearingsService.getHearingByIdWithValidation(hearingId)).thenReturn(mockHearing);

        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.OVERNIGHT;
        when(mockTranscriptionUrgencyRepository.getReferenceById(transcriptionUrgencyEnum.getId()))
            .thenReturn(mockTranscriptionUrgency);

        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.COURT_LOG;
        when(mockTranscriptionTypeRepository.getReferenceById(transcriptionTypeEnum.getId()))
            .thenReturn(mockTranscriptionType);

        when(mockTranscriptionStatusRepository.getReferenceById(REQUESTED.getId()))
            .thenReturn(requestedTranscriptionStatus);
        when(mockTranscriptionStatusRepository.getReferenceById(AWAITING_AUTHORISATION.getId()))
            .thenReturn(awaitingAuthorisationTranscriptionStatus);

        when(mockTranscriptionRepository.saveAndFlush(any(TranscriptionEntity.class))).thenReturn(mockTranscription);
        //when(mockTranscriptionWorkflowEntity.getId()).thenReturn(1,2);
        when(mockTranscriptionWorkflowRepository.saveAndFlush(any())).thenReturn(mockTranscriptionWorkflowEntity);

        mockTranscriptionType.setId(transcriptionTypeEnum.getId());
        when(mockTranscription.getIsManualTranscription()).thenReturn(true);

        when(mockTranscription.getCourtCase()).thenReturn(mockCourtCase);
        doNothing().when(mockAuditApi).record(REQUEST_TRANSCRIPTION, testUser, mockCourtCase);

        Integer caseId = null;
        OffsetDateTime startDateTime = CommonTestDataUtil.createOffsetDateTime(START_TIME);
        OffsetDateTime endDateTime = CommonTestDataUtil.createOffsetDateTime(END_TIME);

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            hearingId,
            caseId,
            transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(),
            TEST_COMMENT,
            startDateTime,
            endDateTime);

        transcriptionService.saveTranscriptionRequest(transcriptionRequestDetails, true);

        verify(duplicateRequestDetector).checkForDuplicate(transcriptionRequestDetails, true);
        verify(mockTranscriptionRepository).saveAndFlush(transcriptionEntityArgumentCaptor.capture());

        TranscriptionEntity transcriptionEntity = transcriptionEntityArgumentCaptor.getValue();
        assertThat(transcriptionEntity.getHearing()).isNotNull();
        assertThat(transcriptionEntity.getCourtCase()).isNotNull();
        assertThat(transcriptionEntity.getCourtroom()).isEqualTo(mockHearing.getCourtroom());
        assertThat(transcriptionEntity.getHearingDate()).isNull();
        assertThat(transcriptionEntity.getTranscriptionStatus()).isEqualTo(requestedTranscriptionStatus);
        assertThat(transcriptionEntity.getTranscriptionType()).isEqualTo(mockTranscriptionType);
        assertThat(transcriptionEntity.getTranscriptionUrgency()).isEqualTo(mockTranscriptionUrgency);
        assertThat(transcriptionEntity.getStartTime()).isEqualTo(startDateTime);
        assertThat(transcriptionEntity.getEndTime()).isEqualTo(endDateTime);
        assertThat(transcriptionEntity.getIsManualTranscription()).isTrue();
        assertThat(transcriptionEntity.getIsCurrent()).isTrue();

        verify(
            mockTranscriptionWorkflowRepository,
            times(2)
        ).saveAndFlush(transcriptionWorkflowEntityArgumentCaptor.capture());

        verify(
            mockTranscriptionCommentRepository,
            times(1)
        ).saveAndFlush(transcriptionCommentEntityArgumentCaptor.capture());

        List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = transcriptionWorkflowEntityArgumentCaptor.getAllValues();
        TranscriptionWorkflowEntity requestedTranscriptionWorkflowEntity = transcriptionWorkflowEntities.getFirst();
        assertThat(requestedTranscriptionWorkflowEntity.getTranscriptionStatus().getId()).isEqualTo(REQUESTED.getId());
        TranscriptionWorkflowEntity awaitingAuthorisationTranscriptionWorkflowEntity = transcriptionWorkflowEntities.get(
            1);
        assertThat(awaitingAuthorisationTranscriptionWorkflowEntity.getTranscriptionStatus().getId()).isEqualTo(
            AWAITING_AUTHORISATION.getId());

        verifyTranscriptionLinkedCaseEntity(mockTranscription, mockCourtCase);
        assertTranscriptionComments();
        verifyNotification();

        verify(mockAuditApi).record(REQUEST_TRANSCRIPTION, testUser, mockCourtCase);
    }

    @Test
    void saveTranscriptionRequestWithValidValuesNullDatesAndSentencingRemarksTypeReturnSuccess() {
        doNothing().when(duplicateRequestDetector).checkForDuplicate(any(TranscriptionRequestDetails.class), any(Boolean.class));

        Integer hearingId = 1;
        when(mockHearingsService.getHearingByIdWithValidation(hearingId)).thenReturn(mockHearing);

        Integer caseId = 1;
        when(mockCaseService.getCourtCaseById(caseId)).thenReturn(mockCourtCase);

        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.OVERNIGHT;
        when(mockTranscriptionUrgencyRepository.getReferenceById(transcriptionUrgencyEnum.getId()))
            .thenReturn(mockTranscriptionUrgency);

        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.SENTENCING_REMARKS;
        when(mockTranscriptionTypeRepository.getReferenceById(transcriptionTypeEnum.getId()))
            .thenReturn(mockTranscriptionType);

        when(mockTranscriptionStatusRepository.getReferenceById(REQUESTED.getId()))
            .thenReturn(requestedTranscriptionStatus);
        when(mockTranscriptionStatusRepository.getReferenceById(AWAITING_AUTHORISATION.getId()))
            .thenReturn(awaitingAuthorisationTranscriptionStatus);

        when(mockTranscriptionRepository.saveAndFlush(any(TranscriptionEntity.class))).thenReturn(mockTranscription);
        when(mockTranscriptionWorkflowRepository.saveAndFlush(any())).thenReturn(mockTranscriptionWorkflowEntity);

        mockTranscriptionType.setId(transcriptionTypeEnum.getId());
        when(mockTranscription.getIsManualTranscription()).thenReturn(true);

        when(mockTranscription.getCourtCase()).thenReturn(mockCourtCase);
        doNothing().when(mockAuditApi).record(REQUEST_TRANSCRIPTION, testUser, mockCourtCase);

        OffsetDateTime startDateTime = null;
        OffsetDateTime endDateTime = null;

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            hearingId,
            caseId,
            transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(),
            TEST_COMMENT,
            startDateTime,
            endDateTime
        );

        transcriptionService.saveTranscriptionRequest(transcriptionRequestDetails, false);

        verify(duplicateRequestDetector).checkForDuplicate(transcriptionRequestDetails, false);
        verify(mockTranscriptionRepository).saveAndFlush(transcriptionEntityArgumentCaptor.capture());

        TranscriptionEntity transcriptionEntity = transcriptionEntityArgumentCaptor.getValue();
        assertThat(transcriptionEntity.getHearing()).isNotNull();
        assertThat(transcriptionEntity.getCourtCase()).isNotNull();
        assertThat(transcriptionEntity.getCourtroom()).isEqualTo(mockHearing.getCourtroom());
        assertThat(transcriptionEntity.getHearingDate()).isNull();
        assertThat(transcriptionEntity.getTranscriptionStatus()).isEqualTo(requestedTranscriptionStatus);
        assertThat(transcriptionEntity.getTranscriptionType()).isEqualTo(mockTranscriptionType);
        assertThat(transcriptionEntity.getTranscriptionUrgency()).isEqualTo(mockTranscriptionUrgency);
        assertThat(transcriptionEntity.getStartTime()).isEqualTo(startDateTime);
        assertThat(transcriptionEntity.getEndTime()).isEqualTo(endDateTime);
        assertThat(transcriptionEntity.getIsManualTranscription()).isFalse();
        assertThat(transcriptionEntity.getIsCurrent()).isTrue();

        verify(
            mockTranscriptionWorkflowRepository,
            times(2)
        ).saveAndFlush(transcriptionWorkflowEntityArgumentCaptor.capture());

        verify(
            mockTranscriptionCommentRepository,
            times(1)
        ).saveAndFlush(transcriptionCommentEntityArgumentCaptor.capture());

        List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = transcriptionWorkflowEntityArgumentCaptor.getAllValues();
        TranscriptionWorkflowEntity requestedTranscriptionWorkflowEntity = transcriptionWorkflowEntities.getFirst();
        assertThat(requestedTranscriptionWorkflowEntity.getTranscriptionStatus().getId()).isEqualTo(REQUESTED.getId());
        TranscriptionWorkflowEntity awaitingAuthorisationTranscriptionWorkflowEntity = transcriptionWorkflowEntities.get(
            1);
        assertThat(awaitingAuthorisationTranscriptionWorkflowEntity.getTranscriptionStatus().getId()).isEqualTo(
            AWAITING_AUTHORISATION.getId());

        verifyTranscriptionLinkedCaseEntity(mockTranscription, mockCourtCase);
        assertTranscriptionComments();
        verifyNotification();


        verify(mockAuditApi).record(REQUEST_TRANSCRIPTION, testUser, mockCourtCase);
    }

    private void assertTranscriptionComments() {
        List<TranscriptionCommentEntity> transcriptionCommentEntities = transcriptionCommentEntityArgumentCaptor.getAllValues();

        assertThat(transcriptionCommentEntities)
            .hasSize(1)
            .extracting(TranscriptionCommentEntity::getComment)
            .containsExactly(TEST_COMMENT);
    }

    @Test
    void saveTranscriptionRequestWithNullCaseAndNullHearingAndCourtLogTypeThrowsException() {

        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.OVERNIGHT;
        when(mockTranscriptionUrgencyRepository.getReferenceById(transcriptionUrgencyEnum.getId())).thenReturn(
            mockTranscriptionUrgency);

        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.COURT_LOG;
        when(mockTranscriptionTypeRepository.getReferenceById(transcriptionTypeEnum.getId()))
            .thenReturn(mockTranscriptionType);

        OffsetDateTime startDateTime = CommonTestDataUtil.createOffsetDateTime(START_TIME);
        OffsetDateTime endDateTime = CommonTestDataUtil.createOffsetDateTime(END_TIME);

        Integer hearingId = null;
        Integer caseId = null;

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            hearingId,
            caseId,
            transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(),
            TEST_COMMENT,
            startDateTime,
            endDateTime
        );
        var exception = assertThrows(
            DartsApiException.class,
            () ->
                transcriptionService.saveTranscriptionRequest(transcriptionRequestDetails, false)
        );

        assertEquals(TranscriptionApiError.FAILED_TO_VALIDATE_TRANSCRIPTION_REQUEST, exception.getError());
    }

    @Test
    void testGetAllCaseTranscriptionDocuments() {
        TranscriptionDocumentEntity transcriptionDoc1 = new TranscriptionDocumentEntity();
        transcriptionDoc1.setId(1);
        TranscriptionDocumentEntity transcriptionDoc2 = new TranscriptionDocumentEntity();
        transcriptionDoc2.setId(2);
        TranscriptionDocumentEntity transcriptionDoc3 = new TranscriptionDocumentEntity();
        transcriptionDoc3.setId(3);

        when(mockTranscriptionRepository.findByCaseIdManualOrLegacy(CASE_ID, true)).thenReturn(List.of(mockTranscription, mockTranscription2));
        when(mockTranscription.getTranscriptionDocumentEntities()).thenReturn(List.of(transcriptionDoc1, transcriptionDoc2));
        when(mockTranscription2.getTranscriptionDocumentEntities()).thenReturn(List.of(transcriptionDoc1, transcriptionDoc3));

        var allCaseTranscriptionDocuments = transcriptionService.getAllCaseTranscriptionDocuments(CASE_ID);

        assertThat(allCaseTranscriptionDocuments).containsExactlyInAnyOrder(transcriptionDoc1, transcriptionDoc2, transcriptionDoc3);
    }

    @Test
    void testRollbackUserTransactions() {
        UserAccountEntity entity = new UserAccountEntity();

        Integer transcriptionId = 1000;
        TranscriptionEntity transcriptionEntity = new TranscriptionEntity();
        transcriptionEntity.setId(transcriptionId);

        TranscriptionStatusEntity transcriptionStatusEntity = new TranscriptionStatusEntity();
        transcriptionEntity.setTranscriptionStatus(transcriptionStatusEntity);

        TranscriptionWorkflowEntity transcriptionWorkflowEntityEntity = new TranscriptionWorkflowEntity();
        transcriptionWorkflowEntityEntity.setTranscription(transcriptionEntity);

        when(mockTranscriptionStatusRepository.getReferenceById(APPROVED.getId())).thenReturn(transcriptionStatusEntity);
        when(mockTranscriptionWorkflowRepository
                 .findWorkflowForUserWithTranscriptionState(entity.getId(), TranscriptionStatusEnum.WITH_TRANSCRIBER.getId()))
            .thenReturn(Arrays.asList(transcriptionEntity));

        when(mockTranscriptionWorkflowRepository.saveAndFlush(Mockito.notNull()))
            .thenReturn(transcriptionWorkflowEntityEntity);

        var allCaseTranscriptionDocuments = transcriptionService.rollbackUserTranscriptions(entity);

        verify(mockTranscriptionWorkflowRepository).saveAndFlush(transcriptionWorkflowEntityArgumentCaptor.capture());
        TranscriptionWorkflowEntity workflowEntity = transcriptionWorkflowEntityArgumentCaptor.getValue();
        assertEquals(transcriptionId, allCaseTranscriptionDocuments.getFirst());
        assertEquals(transcriptionStatusEntity, workflowEntity.getTranscriptionStatus());
    }

    @Test
    void adminGetTranscriptionDocumentsMarkedForDeletionManualDeletionDisabled() {
        updateManualDeletion(false);
        DartsApiException dartsApiException = assertThrows(
            DartsApiException.class, () -> transcriptionService.adminGetTranscriptionDocumentsMarkedForDeletion());
        assertThat(dartsApiException.getError()).isEqualTo(CommonApiError.FEATURE_FLAG_NOT_ENABLED);
    }

    @Test
    void getTranscriptionMapsCurrentTranscriptionWithDocument() {
        var transcriptionId = 1;
        var transcription = new TranscriptionEntity();
        transcription.setIsCurrent(true);
        var transcriptionDocument = new TranscriptionDocumentEntity();
        transcription.setTranscriptionDocumentEntities(List.of(transcriptionDocument));
        when(mockTranscriptionRepository.findById(transcriptionId)).thenReturn(Optional.of(transcription));
        when(transcriptionResponseMapper.mapToTranscriptionResponse(any(TranscriptionEntity.class))).thenReturn(new GetTranscriptionByIdResponse());

        transcriptionService.getTranscription(transcriptionId);
        verify(transcriptionResponseMapper).mapToTranscriptionResponse(transcription);
    }

    @Test
    void getTranscriptionMapsCurrentTranscriptionWithHiddenDocumentIfUserIsSuperAdmin() {
        when(mockUserIdentity.userHasGlobalAccess(Set.of(SUPER_ADMIN))).thenReturn(true);
        var transcriptionId = 1;
        var transcription = new TranscriptionEntity();
        transcription.setIsCurrent(true);
        var transcriptionDocument = new TranscriptionDocumentEntity();
        transcriptionDocument.setHidden(true);
        transcription.setTranscriptionDocumentEntities(List.of(transcriptionDocument));
        when(mockTranscriptionRepository.findById(transcriptionId)).thenReturn(Optional.of(transcription));
        when(transcriptionResponseMapper.mapToTranscriptionResponse(any(TranscriptionEntity.class))).thenReturn(new GetTranscriptionByIdResponse());

        transcriptionService.getTranscription(transcriptionId);
        verify(transcriptionResponseMapper).mapToTranscriptionResponse(transcription);
    }

    @Test
    void getTranscriptionThrowsNotFoundExceptionIfTranscriptionDocumentHiddenAndUserIsNotSuperAdmin() {
        var transcriptionId = 1;
        var transcription = new TranscriptionEntity();
        transcription.setIsCurrent(true);
        transcription.setId(transcriptionId);
        var transcriptionDocument = new TranscriptionDocumentEntity();
        transcriptionDocument.setHidden(true);
        transcription.setTranscriptionDocumentEntities(List.of(transcriptionDocument));
        when(mockTranscriptionRepository.findById(transcriptionId)).thenReturn(Optional.of(transcription));
        when(mockTranscriptionDocumentRepository.findByTranscriptionIdAndHiddenTrueIncludeDeleted(anyInt()))
            .thenReturn(List.of(new TranscriptionDocumentEntity()));

        assertThatThrownBy(() -> transcriptionService.getTranscription(transcriptionId))
            .isExactlyInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", TRANSCRIPTION_NOT_FOUND);
        verify(mockTranscriptionDocumentRepository).findByTranscriptionIdAndHiddenTrueIncludeDeleted(transcriptionId);
    }

    private TranscriptionRequestDetails createTranscriptionRequestDetails(Integer hearingId,
                                                                          Integer caseId,
                                                                          Integer urgencyId,
                                                                          Integer transcriptionTypeId,
                                                                          String comment,
                                                                          OffsetDateTime startDateTime,
                                                                          OffsetDateTime endDateTime
    ) {
        TranscriptionRequestDetails transcriptionRequestDetails = new TranscriptionRequestDetails();
        transcriptionRequestDetails.setHearingId(hearingId);
        transcriptionRequestDetails.setCaseId(caseId);
        transcriptionRequestDetails.setTranscriptionUrgencyId(urgencyId);
        transcriptionRequestDetails.setTranscriptionTypeId(transcriptionTypeId);
        transcriptionRequestDetails.setComment(comment);
        transcriptionRequestDetails.setStartDateTime(startDateTime);
        transcriptionRequestDetails.setEndDateTime(endDateTime);
        return transcriptionRequestDetails;
    }

    private void verifyNotification() {
        verify(transcriptionNotifications, times(1)).notifyApprovers(any(TranscriptionEntity.class));
    }

    private void verifyTranscriptionLinkedCaseEntity(TranscriptionEntity mockTranscription, CourtCaseEntity mockCourtCase) {
        verify(transcriptionLinkedCaseRepository)
            .save(transcriptionLinkedCaseEntityArgumentCaptor.capture());

        TranscriptionLinkedCaseEntity transcriptionLinkedCaseEntity = transcriptionLinkedCaseEntityArgumentCaptor.getValue();
        assertThat(transcriptionLinkedCaseEntity.getTranscription()).isEqualTo(mockTranscription);
        assertThat(transcriptionLinkedCaseEntity.getCourtCase()).isEqualTo(mockCourtCase);
    }
}