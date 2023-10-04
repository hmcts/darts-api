package uk.gov.hmcts.darts.transcriptions.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionStatusRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionTypeRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionUrgencyRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionWorkflowRepository;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.hearings.service.HearingsService;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;
import uk.gov.hmcts.darts.transcriptions.validator.WorkflowValidator;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
    private CaseService mockCaseService;
    @Mock
    private HearingsService mockHearingsService;
    @Mock
    private UserIdentity mockUserIdentity;
    @Mock
    private WorkflowValidator mockWorkflowValidator;

    private HearingEntity mockHearing;
    private CourtCaseEntity mockCourtCase;
    private TranscriptionUrgencyEntity mockTranscriptionUrgency;
    private TranscriptionTypeEntity mockTranscriptionType;
    private TranscriptionStatusEntity requestedTranscriptionStatus;
    private TranscriptionStatusEntity awaitingAuthorisationTranscriptionStatus;

    @Mock
    private TranscriptionEntity mockTranscription;

    @InjectMocks
    private TranscriptionServiceImpl transcriptionService;

    @Captor
    private ArgumentCaptor<TranscriptionEntity> transcriptionEntityArgumentCaptor;

    @Captor
    private ArgumentCaptor<TranscriptionWorkflowEntity> transcriptionWorkflowEntityArgumentCaptor;

    @BeforeEach
    void setUp() {

        UserAccountEntity testUser = new UserAccountEntity();
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
    }

    @Test
    void saveTranscriptionRequestWithValidValuesAndCourtLogTypeReturnSuccess() {

        Integer hearingId = 1;
        when(mockHearingsService.getHearingById(hearingId)).thenReturn(mockHearing);

        Integer caseId = 1;
        when(mockCaseService.getCourtCaseById(caseId)).thenReturn(mockCourtCase);

        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;
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
        mockTranscriptionType.setId(transcriptionTypeEnum.getId());
        when(mockTranscription.getTranscriptionType()).thenReturn(mockTranscriptionType);
        when(mockWorkflowValidator.isAutomatedTranscription(transcriptionTypeEnum)).thenReturn(false);

        String comment = TEST_COMMENT;
        OffsetDateTime startDateTime = CommonTestDataUtil.createOffsetDateTime(START_TIME);
        OffsetDateTime endDateTime = CommonTestDataUtil.createOffsetDateTime(END_TIME);

        transcriptionService.saveTranscriptionRequest(createTranscriptionRequestDetails(
            hearingId,
            caseId,
            transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(),
            comment,
            startDateTime,
            endDateTime
        ));

        verify(mockTranscriptionRepository).saveAndFlush(transcriptionEntityArgumentCaptor.capture());

        TranscriptionEntity transcriptionEntity = transcriptionEntityArgumentCaptor.getValue();
        assertThat(transcriptionEntity.getHearing()).isNotNull();
        assertThat(transcriptionEntity.getCourtCase()).isNotNull();
        assertThat(transcriptionEntity.getCourtroom()).isNull();
        assertThat(transcriptionEntity.getHearingDate()).isNull();
        assertThat(transcriptionEntity.getTranscriptionStatus()).isEqualTo(requestedTranscriptionStatus);
        assertThat(transcriptionEntity.getTranscriptionType()).isEqualTo(mockTranscriptionType);
        assertThat(transcriptionEntity.getTranscriptionUrgency()).isEqualTo(mockTranscriptionUrgency);
        assertThat(transcriptionEntity.getStart()).isEqualTo(startDateTime);
        assertThat(transcriptionEntity.getEnd()).isEqualTo(endDateTime);

        verify(
            mockTranscriptionWorkflowRepository,
            times(2)
        ).saveAndFlush(transcriptionWorkflowEntityArgumentCaptor.capture());

        List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = transcriptionWorkflowEntityArgumentCaptor.getAllValues();
        TranscriptionWorkflowEntity requestedTranscriptionWorkflowEntity = transcriptionWorkflowEntities.get(0);
        assertThat(requestedTranscriptionWorkflowEntity.getWorkflowComment()).isEqualTo(comment);
        assertThat(requestedTranscriptionWorkflowEntity.getTranscriptionStatus().getId()).isEqualTo(REQUESTED.getId());
        TranscriptionWorkflowEntity awaitingAuthorisationTranscriptionWorkflowEntity = transcriptionWorkflowEntities.get(
            1);
        assertThat(awaitingAuthorisationTranscriptionWorkflowEntity.getWorkflowComment()).isNull();
        assertThat(awaitingAuthorisationTranscriptionWorkflowEntity.getTranscriptionStatus().getId()).isEqualTo(
            AWAITING_AUTHORISATION.getId());
    }

    @Test
    void saveTranscriptionRequestWithValidValuesNullHearingAndCourtLogTypeReturnSuccess() {

        Integer caseId = 1;
        when(mockCaseService.getCourtCaseById(caseId)).thenReturn(mockCourtCase);

        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;
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
        mockTranscriptionType.setId(transcriptionTypeEnum.getId());
        when(mockTranscription.getTranscriptionType()).thenReturn(mockTranscriptionType);
        when(mockWorkflowValidator.isAutomatedTranscription(transcriptionTypeEnum)).thenReturn(false);

        Integer hearingId = null;
        String comment = TEST_COMMENT;
        OffsetDateTime startDateTime = CommonTestDataUtil.createOffsetDateTime(START_TIME);
        OffsetDateTime endDateTime = CommonTestDataUtil.createOffsetDateTime(END_TIME);

        transcriptionService.saveTranscriptionRequest(createTranscriptionRequestDetails(
            hearingId,
            caseId,
            transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(),
            comment,
            startDateTime,
            endDateTime
        ));

        verify(mockTranscriptionRepository).saveAndFlush(transcriptionEntityArgumentCaptor.capture());

        TranscriptionEntity transcriptionEntity = transcriptionEntityArgumentCaptor.getValue();
        assertThat(transcriptionEntity.getHearing()).isNull();
        assertThat(transcriptionEntity.getCourtCase()).isNotNull();
        assertThat(transcriptionEntity.getCourtroom()).isNull();
        assertThat(transcriptionEntity.getTranscriptionUrgency()).isNotNull();
        assertThat(transcriptionEntity.getStart()).isEqualTo(startDateTime);
        assertThat(transcriptionEntity.getEnd()).isEqualTo(endDateTime);

        verify(
            mockTranscriptionWorkflowRepository,
            times(2)
        ).saveAndFlush(transcriptionWorkflowEntityArgumentCaptor.capture());

        List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = transcriptionWorkflowEntityArgumentCaptor.getAllValues();
        TranscriptionWorkflowEntity requestedTranscriptionWorkflowEntity = transcriptionWorkflowEntities.get(0);
        assertThat(requestedTranscriptionWorkflowEntity.getWorkflowComment()).isEqualTo(comment);
        assertThat(requestedTranscriptionWorkflowEntity.getTranscriptionStatus().getId()).isEqualTo(REQUESTED.getId());
        TranscriptionWorkflowEntity awaitingAuthorisationTranscriptionWorkflowEntity = transcriptionWorkflowEntities.get(
            1);
        assertThat(awaitingAuthorisationTranscriptionWorkflowEntity.getWorkflowComment()).isNull();
        assertThat(awaitingAuthorisationTranscriptionWorkflowEntity.getTranscriptionStatus().getId()).isEqualTo(
            AWAITING_AUTHORISATION.getId());
    }

    @Test
    void saveTranscriptionRequestWithValidValuesNullCourtCaseAndCourtLogTypeReturnSuccess() {

        Integer hearingId = 1;
        when(mockHearingsService.getHearingById(hearingId)).thenReturn(mockHearing);

        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;
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
        mockTranscriptionType.setId(transcriptionTypeEnum.getId());
        when(mockTranscription.getTranscriptionType()).thenReturn(mockTranscriptionType);
        when(mockWorkflowValidator.isAutomatedTranscription(transcriptionTypeEnum)).thenReturn(false);

        Integer caseId = null;
        String comment = TEST_COMMENT;
        OffsetDateTime startDateTime = CommonTestDataUtil.createOffsetDateTime(START_TIME);
        OffsetDateTime endDateTime = CommonTestDataUtil.createOffsetDateTime(END_TIME);

        transcriptionService.saveTranscriptionRequest(createTranscriptionRequestDetails(
            hearingId,
            caseId,
            transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(),
            comment,
            startDateTime,
            endDateTime
        ));

        verify(mockTranscriptionRepository).saveAndFlush(transcriptionEntityArgumentCaptor.capture());

        TranscriptionEntity transcriptionEntity = transcriptionEntityArgumentCaptor.getValue();
        assertThat(transcriptionEntity.getHearing()).isNotNull();
        assertThat(transcriptionEntity.getCourtCase()).isNotNull();
        assertThat(transcriptionEntity.getCourtroom()).isNull();
        assertThat(transcriptionEntity.getHearingDate()).isNull();
        assertThat(transcriptionEntity.getTranscriptionStatus()).isEqualTo(requestedTranscriptionStatus);
        assertThat(transcriptionEntity.getTranscriptionType()).isEqualTo(mockTranscriptionType);
        assertThat(transcriptionEntity.getTranscriptionUrgency()).isEqualTo(mockTranscriptionUrgency);
        assertThat(transcriptionEntity.getStart()).isEqualTo(startDateTime);
        assertThat(transcriptionEntity.getEnd()).isEqualTo(endDateTime);

        verify(
            mockTranscriptionWorkflowRepository,
            times(2)
        ).saveAndFlush(transcriptionWorkflowEntityArgumentCaptor.capture());

        List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = transcriptionWorkflowEntityArgumentCaptor.getAllValues();
        TranscriptionWorkflowEntity requestedTranscriptionWorkflowEntity = transcriptionWorkflowEntities.get(0);
        assertThat(requestedTranscriptionWorkflowEntity.getWorkflowComment()).isEqualTo(comment);
        assertThat(requestedTranscriptionWorkflowEntity.getTranscriptionStatus().getId()).isEqualTo(REQUESTED.getId());
        TranscriptionWorkflowEntity awaitingAuthorisationTranscriptionWorkflowEntity = transcriptionWorkflowEntities.get(
            1);
        assertThat(awaitingAuthorisationTranscriptionWorkflowEntity.getWorkflowComment()).isNull();
        assertThat(awaitingAuthorisationTranscriptionWorkflowEntity.getTranscriptionStatus().getId()).isEqualTo(
            AWAITING_AUTHORISATION.getId());
    }

    @Test
    void saveTranscriptionRequestWithValidValuesNullDatesAndSentencingRemarksTypeReturnSuccess() {

        Integer hearingId = 1;
        when(mockHearingsService.getHearingById(hearingId)).thenReturn(mockHearing);

        Integer caseId = 1;
        when(mockCaseService.getCourtCaseById(caseId)).thenReturn(mockCourtCase);

        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;
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
        mockTranscriptionType.setId(transcriptionTypeEnum.getId());
        when(mockTranscription.getTranscriptionType()).thenReturn(mockTranscriptionType);
        when(mockWorkflowValidator.isAutomatedTranscription(transcriptionTypeEnum)).thenReturn(false);

        String comment = TEST_COMMENT;
        OffsetDateTime startDateTime = null;
        OffsetDateTime endDateTime = null;

        transcriptionService.saveTranscriptionRequest(createTranscriptionRequestDetails(
            hearingId,
            caseId,
            transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(),
            comment,
            startDateTime,
            endDateTime
        ));

        verify(mockTranscriptionRepository).saveAndFlush(transcriptionEntityArgumentCaptor.capture());

        TranscriptionEntity transcriptionEntity = transcriptionEntityArgumentCaptor.getValue();
        assertThat(transcriptionEntity.getHearing()).isNotNull();
        assertThat(transcriptionEntity.getCourtCase()).isNotNull();
        assertThat(transcriptionEntity.getCourtroom()).isNull();
        assertThat(transcriptionEntity.getHearingDate()).isNull();
        assertThat(transcriptionEntity.getTranscriptionStatus()).isEqualTo(requestedTranscriptionStatus);
        assertThat(transcriptionEntity.getTranscriptionType()).isEqualTo(mockTranscriptionType);
        assertThat(transcriptionEntity.getTranscriptionUrgency()).isEqualTo(mockTranscriptionUrgency);
        assertThat(transcriptionEntity.getStart()).isEqualTo(startDateTime);
        assertThat(transcriptionEntity.getEnd()).isEqualTo(endDateTime);

        verify(
            mockTranscriptionWorkflowRepository,
            times(2)
        ).saveAndFlush(transcriptionWorkflowEntityArgumentCaptor.capture());

        List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = transcriptionWorkflowEntityArgumentCaptor.getAllValues();
        TranscriptionWorkflowEntity requestedTranscriptionWorkflowEntity = transcriptionWorkflowEntities.get(0);
        assertThat(requestedTranscriptionWorkflowEntity.getWorkflowComment()).isEqualTo(comment);
        assertThat(requestedTranscriptionWorkflowEntity.getTranscriptionStatus().getId()).isEqualTo(REQUESTED.getId());
        TranscriptionWorkflowEntity awaitingAuthorisationTranscriptionWorkflowEntity = transcriptionWorkflowEntities.get(
            1);
        assertThat(awaitingAuthorisationTranscriptionWorkflowEntity.getWorkflowComment()).isNull();
        assertThat(awaitingAuthorisationTranscriptionWorkflowEntity.getTranscriptionStatus().getId()).isEqualTo(
            AWAITING_AUTHORISATION.getId());
    }

    @Test
    void saveTranscriptionRequestWithNullCaseAndNullHearingAndCourtLogTypeThrowsException() {

        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;
        when(mockTranscriptionUrgencyRepository.getReferenceById(transcriptionUrgencyEnum.getId())).thenReturn(
            mockTranscriptionUrgency);

        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.COURT_LOG;
        when(mockTranscriptionTypeRepository.getReferenceById(transcriptionTypeEnum.getId()))
            .thenReturn(mockTranscriptionType);

        String comment = TEST_COMMENT;
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
                    comment,
                    startDateTime,
                    endDateTime
                ))
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
}
