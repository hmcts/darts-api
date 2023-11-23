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
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.TranscriptionCommentRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionStatusRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionTypeRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionUrgencyRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionWorkflowRepository;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.hearings.service.HearingsService;
import uk.gov.hmcts.darts.notification.api.NotificationApi;
import uk.gov.hmcts.darts.notification.dto.SaveNotificationToDbRequest;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;
import uk.gov.hmcts.darts.transcriptions.validator.WorkflowValidator;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.REQUEST_TRANSCRIPTION;
import static uk.gov.hmcts.darts.notification.api.NotificationApi.NotificationTemplate.COURT_MANAGER_APPROVE_TRANSCRIPT;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;

@Slf4j
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.LawOfDemeter", "PMD.ExcessiveImports"})
class TranscriptionServiceImplTest {

    private static final String TEST_COMMENT = "Test comment";
    private static final String START_TIME = "2023-07-01T09:00:00";
    private static final String END_TIME = "2023-07-01T12:00:00";

    @Mock
    private TranscriptionRepository mockTranscriptionRepository;
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
    private WorkflowValidator mockWorkflowValidator;
    @Mock
    private AuthorisationApi authorisationApi;
    @Mock
    private NotificationApi notificationApi;

    private HearingEntity mockHearing;
    private CourtCaseEntity mockCourtCase;
    private TranscriptionUrgencyEntity mockTranscriptionUrgency;
    private TranscriptionTypeEntity mockTranscriptionType;
    private TranscriptionStatusEntity requestedTranscriptionStatus;
    private TranscriptionStatusEntity awaitingAuthorisationTranscriptionStatus;
    private UserAccountEntity testUser;
    private List<UserAccountEntity> approvers;

    @Mock
    private TranscriptionEntity mockTranscription;

    @InjectMocks
    private TranscriptionServiceImpl transcriptionService;

    @Captor
    private ArgumentCaptor<TranscriptionEntity> transcriptionEntityArgumentCaptor;

    @Captor
    private ArgumentCaptor<TranscriptionWorkflowEntity> transcriptionWorkflowEntityArgumentCaptor;

    @Captor
    private ArgumentCaptor<TranscriptionCommentEntity> transcriptionCommentEntityArgumentCaptor;

    @BeforeEach
    void setUp() {

        testUser = new UserAccountEntity();
        testUser.setEmailAddress("test.user@example.com");
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

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

        approvers = new ArrayList<>();
        UserAccountEntity approver1 = new UserAccountEntity();
        UserAccountEntity approver2 = new UserAccountEntity();
        approver1.setEmailAddress("approver1@example.com");
        approver2.setEmailAddress("approver2@example.com");
        approvers.add(approver1);
        approvers.add(approver2);

        Mockito.lenient().when(authorisationApi.getUsersWithRoleAtCourthouse(eq(SecurityRoleEnum.APPROVER), any())).thenReturn(approvers);
    }

    @Test
    void saveTranscriptionRequestWithValidValuesAndCourtLogTypeReturnSuccess() {

        Integer hearingId = 1;
        when(mockHearingsService.getHearingById(hearingId)).thenReturn(mockHearing);

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
        doNothing().when(mockAuditApi).recordAudit(REQUEST_TRANSCRIPTION, testUser, mockCourtCase);

        OffsetDateTime startDateTime = CommonTestDataUtil.createOffsetDateTime(START_TIME);
        OffsetDateTime endDateTime = CommonTestDataUtil.createOffsetDateTime(END_TIME);

        transcriptionService.saveTranscriptionRequest(createTranscriptionRequestDetails(
            hearingId,
            caseId,
            transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(),
            TEST_COMMENT,
            startDateTime,
            endDateTime
        ), true);

        verify(mockTranscriptionRepository).saveAndFlush(transcriptionEntityArgumentCaptor.capture());

        TranscriptionEntity transcriptionEntity = transcriptionEntityArgumentCaptor.getValue();
        assertThat(transcriptionEntity.getHearing()).isNotNull();
        assertThat(transcriptionEntity.getCourtCase()).isNotNull();
        assertThat(transcriptionEntity.getCourtroom()).isNull();
        assertThat(transcriptionEntity.getHearingDate()).isNull();
        assertThat(transcriptionEntity.getTranscriptionStatus()).isEqualTo(requestedTranscriptionStatus);
        assertThat(transcriptionEntity.getTranscriptionType()).isEqualTo(mockTranscriptionType);
        assertThat(transcriptionEntity.getTranscriptionUrgency()).isEqualTo(mockTranscriptionUrgency);
        assertThat(transcriptionEntity.getStartTime()).isEqualTo(startDateTime);
        assertThat(transcriptionEntity.getEndTime()).isEqualTo(endDateTime);
        assertThat(transcriptionEntity.getIsManualTranscription()).isTrue();

        verify(
            mockTranscriptionWorkflowRepository,
            times(2)
        ).saveAndFlush(transcriptionWorkflowEntityArgumentCaptor.capture());

        verify(
            mockTranscriptionCommentRepository,
            times(1)
        ).saveAndFlush(transcriptionCommentEntityArgumentCaptor.capture());

        List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = transcriptionWorkflowEntityArgumentCaptor.getAllValues();
        TranscriptionWorkflowEntity requestedTranscriptionWorkflowEntity = transcriptionWorkflowEntities.get(0);
        assertThat(requestedTranscriptionWorkflowEntity.getTranscriptionStatus().getId()).isEqualTo(REQUESTED.getId());
        TranscriptionWorkflowEntity awaitingAuthorisationTranscriptionWorkflowEntity = transcriptionWorkflowEntities.get(
            1);
        assertThat(awaitingAuthorisationTranscriptionWorkflowEntity.getTranscriptionStatus().getId()).isEqualTo(
            AWAITING_AUTHORISATION.getId());


        assertTranscriptionComments();
        verifyNotification();

        verify(mockAuditApi).recordAudit(REQUEST_TRANSCRIPTION, testUser, mockCourtCase);
    }

    @Test
    void saveTranscriptionRequestWithValidValuesNullHearingAndCourtLogTypeReturnSuccess() {

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
        doNothing().when(mockAuditApi).recordAudit(REQUEST_TRANSCRIPTION, testUser, mockCourtCase);

        Integer hearingId = null;
        OffsetDateTime startDateTime = CommonTestDataUtil.createOffsetDateTime(START_TIME);
        OffsetDateTime endDateTime = CommonTestDataUtil.createOffsetDateTime(END_TIME);

        transcriptionService.saveTranscriptionRequest(createTranscriptionRequestDetails(
            hearingId,
            caseId,
            transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(),
            TEST_COMMENT,
            startDateTime,
            endDateTime
        ), true);

        verify(mockTranscriptionRepository).saveAndFlush(transcriptionEntityArgumentCaptor.capture());

        TranscriptionEntity transcriptionEntity = transcriptionEntityArgumentCaptor.getValue();
        assertThat(transcriptionEntity.getHearing()).isNull();
        assertThat(transcriptionEntity.getCourtCase()).isNotNull();
        assertThat(transcriptionEntity.getCourtroom()).isNull();
        assertThat(transcriptionEntity.getTranscriptionUrgency()).isNotNull();
        assertThat(transcriptionEntity.getStartTime()).isEqualTo(startDateTime);
        assertThat(transcriptionEntity.getEndTime()).isEqualTo(endDateTime);
        assertThat(transcriptionEntity.getIsManualTranscription()).isTrue();

        verify(
            mockTranscriptionWorkflowRepository,
            times(2)
        ).saveAndFlush(transcriptionWorkflowEntityArgumentCaptor.capture());

        verify(
            mockTranscriptionCommentRepository,
            times(1)
        ).saveAndFlush(transcriptionCommentEntityArgumentCaptor.capture());

        List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = transcriptionWorkflowEntityArgumentCaptor.getAllValues();
        TranscriptionWorkflowEntity requestedTranscriptionWorkflowEntity = transcriptionWorkflowEntities.get(0);
        assertThat(requestedTranscriptionWorkflowEntity.getTranscriptionStatus().getId()).isEqualTo(REQUESTED.getId());
        TranscriptionWorkflowEntity awaitingAuthorisationTranscriptionWorkflowEntity = transcriptionWorkflowEntities.get(
            1);
        assertThat(awaitingAuthorisationTranscriptionWorkflowEntity.getTranscriptionStatus().getId()).isEqualTo(
            AWAITING_AUTHORISATION.getId());

        assertTranscriptionComments();
        verifyNotification();

        verify(mockAuditApi).recordAudit(REQUEST_TRANSCRIPTION, testUser, mockCourtCase);
    }

    @Test
    void saveTranscriptionRequestWithValidValuesNullCourtCaseAndCourtLogTypeReturnSuccess() {

        Integer hearingId = 1;
        when(mockHearingsService.getHearingById(hearingId)).thenReturn(mockHearing);

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
        doNothing().when(mockAuditApi).recordAudit(REQUEST_TRANSCRIPTION, testUser, mockCourtCase);

        Integer caseId = null;
        OffsetDateTime startDateTime = CommonTestDataUtil.createOffsetDateTime(START_TIME);
        OffsetDateTime endDateTime = CommonTestDataUtil.createOffsetDateTime(END_TIME);

        transcriptionService.saveTranscriptionRequest(createTranscriptionRequestDetails(
            hearingId,
            caseId,
            transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(),
            TEST_COMMENT,
            startDateTime,
            endDateTime
        ), true);

        verify(mockTranscriptionRepository).saveAndFlush(transcriptionEntityArgumentCaptor.capture());

        TranscriptionEntity transcriptionEntity = transcriptionEntityArgumentCaptor.getValue();
        assertThat(transcriptionEntity.getHearing()).isNotNull();
        assertThat(transcriptionEntity.getCourtCase()).isNotNull();
        assertThat(transcriptionEntity.getCourtroom()).isNull();
        assertThat(transcriptionEntity.getHearingDate()).isNull();
        assertThat(transcriptionEntity.getTranscriptionStatus()).isEqualTo(requestedTranscriptionStatus);
        assertThat(transcriptionEntity.getTranscriptionType()).isEqualTo(mockTranscriptionType);
        assertThat(transcriptionEntity.getTranscriptionUrgency()).isEqualTo(mockTranscriptionUrgency);
        assertThat(transcriptionEntity.getStartTime()).isEqualTo(startDateTime);
        assertThat(transcriptionEntity.getEndTime()).isEqualTo(endDateTime);
        assertThat(transcriptionEntity.getIsManualTranscription()).isTrue();

        verify(
            mockTranscriptionWorkflowRepository,
            times(2)
        ).saveAndFlush(transcriptionWorkflowEntityArgumentCaptor.capture());

        verify(
            mockTranscriptionCommentRepository,
            times(1)
        ).saveAndFlush(transcriptionCommentEntityArgumentCaptor.capture());

        List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = transcriptionWorkflowEntityArgumentCaptor.getAllValues();
        TranscriptionWorkflowEntity requestedTranscriptionWorkflowEntity = transcriptionWorkflowEntities.get(0);
        assertThat(requestedTranscriptionWorkflowEntity.getTranscriptionStatus().getId()).isEqualTo(REQUESTED.getId());
        TranscriptionWorkflowEntity awaitingAuthorisationTranscriptionWorkflowEntity = transcriptionWorkflowEntities.get(
            1);
        assertThat(awaitingAuthorisationTranscriptionWorkflowEntity.getTranscriptionStatus().getId()).isEqualTo(
            AWAITING_AUTHORISATION.getId());


        assertTranscriptionComments();
        verifyNotification();

        verify(mockAuditApi).recordAudit(REQUEST_TRANSCRIPTION, testUser, mockCourtCase);
    }

    @Test
    void saveTranscriptionRequestWithValidValuesNullDatesAndSentencingRemarksTypeReturnSuccess() {

        Integer hearingId = 1;
        when(mockHearingsService.getHearingById(hearingId)).thenReturn(mockHearing);

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
        doNothing().when(mockAuditApi).recordAudit(REQUEST_TRANSCRIPTION, testUser, mockCourtCase);

        OffsetDateTime startDateTime = null;
        OffsetDateTime endDateTime = null;

        transcriptionService.saveTranscriptionRequest(createTranscriptionRequestDetails(
            hearingId,
            caseId,
            transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(),
            TEST_COMMENT,
            startDateTime,
            endDateTime
        ), false);

        verify(mockTranscriptionRepository).saveAndFlush(transcriptionEntityArgumentCaptor.capture());

        TranscriptionEntity transcriptionEntity = transcriptionEntityArgumentCaptor.getValue();
        assertThat(transcriptionEntity.getHearing()).isNotNull();
        assertThat(transcriptionEntity.getCourtCase()).isNotNull();
        assertThat(transcriptionEntity.getCourtroom()).isNull();
        assertThat(transcriptionEntity.getHearingDate()).isNull();
        assertThat(transcriptionEntity.getTranscriptionStatus()).isEqualTo(requestedTranscriptionStatus);
        assertThat(transcriptionEntity.getTranscriptionType()).isEqualTo(mockTranscriptionType);
        assertThat(transcriptionEntity.getTranscriptionUrgency()).isEqualTo(mockTranscriptionUrgency);
        assertThat(transcriptionEntity.getStartTime()).isEqualTo(startDateTime);
        assertThat(transcriptionEntity.getEndTime()).isEqualTo(endDateTime);
        assertThat(transcriptionEntity.getIsManualTranscription()).isFalse();

        verify(
            mockTranscriptionWorkflowRepository,
            times(2)
        ).saveAndFlush(transcriptionWorkflowEntityArgumentCaptor.capture());

        verify(
            mockTranscriptionCommentRepository,
            times(1)
        ).saveAndFlush(transcriptionCommentEntityArgumentCaptor.capture());

        List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = transcriptionWorkflowEntityArgumentCaptor.getAllValues();
        TranscriptionWorkflowEntity requestedTranscriptionWorkflowEntity = transcriptionWorkflowEntities.get(0);
        assertThat(requestedTranscriptionWorkflowEntity.getTranscriptionStatus().getId()).isEqualTo(REQUESTED.getId());
        TranscriptionWorkflowEntity awaitingAuthorisationTranscriptionWorkflowEntity = transcriptionWorkflowEntities.get(
            1);
        assertThat(awaitingAuthorisationTranscriptionWorkflowEntity.getTranscriptionStatus().getId()).isEqualTo(
            AWAITING_AUTHORISATION.getId());


        assertTranscriptionComments();
        verifyNotification();


        verify(mockAuditApi).recordAudit(REQUEST_TRANSCRIPTION, testUser, mockCourtCase);
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

        var exception = assertThrows(
            DartsApiException.class,
            () ->
                transcriptionService.saveTranscriptionRequest(createTranscriptionRequestDetails(
                    hearingId,
                    caseId,
                    transcriptionUrgencyEnum.getId(),
                    transcriptionTypeEnum.getId(),
                    TEST_COMMENT,
                    startDateTime,
                    endDateTime
                ), false)
        );

        assertEquals(TranscriptionApiError.FAILED_TO_VALIDATE_TRANSCRIPTION_REQUEST, exception.getError());
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
        transcriptionRequestDetails.setUrgencyId(urgencyId);
        transcriptionRequestDetails.setTranscriptionTypeId(transcriptionTypeId);
        transcriptionRequestDetails.setComment(comment);
        transcriptionRequestDetails.setStartDateTime(startDateTime);
        transcriptionRequestDetails.setEndDateTime(endDateTime);
        return transcriptionRequestDetails;
    }

    private void verifyNotification() {
        var saveNotificationToDbRequest = SaveNotificationToDbRequest.builder()
            .eventId(COURT_MANAGER_APPROVE_TRANSCRIPT.toString())
            .userAccountsToEmail(approvers)
            .build();
        verify(notificationApi).scheduleNotification(saveNotificationToDbRequest);
    }

}
