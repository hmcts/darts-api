package uk.gov.hmcts.darts.retention.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.event.model.CreatedHearingAndEvent;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.model.DartsEventRetentionPolicy;
import uk.gov.hmcts.darts.event.model.stopandclosehandler.PendingRetention;
import uk.gov.hmcts.darts.event.service.CaseManagementRetentionService;
import uk.gov.hmcts.darts.retention.api.RetentionApi;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceCategoryEnum;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloseCaseWithRetentionServiceImplTest {

    @Mock
    private CaseRetentionRepository caseRetentionRepository;
    @Mock
    private CaseManagementRetentionService caseManagementRetentionService;
    @Mock
    private AuthorisationApi authorisationApi;
    @Mock
    private RetentionApi retentionApi;
    @Mock
    private CaseRepository caseRepository;

    private CloseCaseWithRetentionServiceImpl service;

    @Mock
    private CreatedHearingAndEvent hearingAndEvent;
    @Mock
    private PendingRetention pendingRetention;

    @Captor
    private ArgumentCaptor<CaseRetentionEntity> caseRetentionCaptor;

    private CourtCaseEntity courtCase;
    private DartsEvent dartsEvent;
    private HearingEntity hearing;
    private UserAccountEntity currentUser;
    private CaseManagementRetentionEntity caseManagementRetention;
    private RetentionPolicyTypeEntity retentionPolicyType;

    @BeforeEach
    void setUp() {
        dartsEvent = new DartsEvent();
        dartsEvent.setEventId("1");

        currentUser = new UserAccountEntity();
        currentUser.setId(123);
        lenient().when(authorisationApi.getCurrentUser()).thenReturn(currentUser);

        courtCase = new CourtCaseEntity();
        courtCase.setId(99);
        courtCase.setClosed(false);

        hearing = new HearingEntity();
        hearing.setCourtCase(courtCase);
        lenient().when(hearingAndEvent.getHearingEntity()).thenReturn(hearing);

        retentionPolicyType = new RetentionPolicyTypeEntity();
        retentionPolicyType.setId(1);

        caseManagementRetention = new CaseManagementRetentionEntity();
        caseManagementRetention.setRetentionPolicyTypeEntity(retentionPolicyType);
        when(caseManagementRetentionService.createCaseManagementRetention(any(), any(), any()))
            .thenReturn(caseManagementRetention);

        service = new CloseCaseWithRetentionServiceImpl(caseRetentionRepository, caseManagementRetentionService,
                                                        authorisationApi, retentionApi, caseRepository);
        ReflectionTestUtils.setField(service, "overridableFixedPolicyKeys",
                                     List.of("OVERRIDABLE_POLICY"));

    }

    @Test
    void closeCaseAndSetRetention_shouldCloseAndSetRetentionOnCase() {
        dartsEvent.setDateTime(OffsetDateTime.now());
        DartsEventRetentionPolicy retentionPolicy = new DartsEventRetentionPolicy();
        retentionPolicy.setCaseRetentionFixedPolicy("DEFAULT");
        dartsEvent.setRetentionPolicy(retentionPolicy);

        CreatedHearingAndEvent hearingAndEvent = mock(CreatedHearingAndEvent.class);
        HearingEntity hearingEntity = mock(HearingEntity.class);
        when(hearingAndEvent.getHearingEntity()).thenReturn(hearingEntity);

        CourtCaseEntity courtCase = new CourtCaseEntity();
        when(hearingEntity.getCourtCase()).thenReturn(new CourtCaseEntity());

        CaseManagementRetentionEntity caseManagementRetentionEntity = mock(CaseManagementRetentionEntity.class);

        when(caseManagementRetentionService.createCaseManagementRetention(
            hearingAndEvent.getEventEntity(),
            hearingEntity.getCourtCase(),
            retentionPolicy))
            .thenReturn(caseManagementRetentionEntity);
        when(caseManagementRetentionEntity.getRetentionPolicyTypeEntity())
            .thenReturn(mock(RetentionPolicyTypeEntity.class));

        when(retentionApi.applyPolicyStringToDate(any(), eq(null), any(RetentionPolicyTypeEntity.class)))
            .thenReturn(LocalDate.now());
        when(caseRetentionRepository.findLatestCompletedManualRetention(courtCase))
            .thenReturn(Optional.empty());
        when(caseRetentionRepository.findLatestPendingRetention(courtCase))
            .thenReturn(Optional.empty());
        when(authorisationApi.getCurrentUser()).thenReturn(mock(UserAccountEntity.class));

        service.closeCaseAndSetRetention(dartsEvent, hearingAndEvent, courtCase);

        verify(caseRepository).saveAndFlush(any(CourtCaseEntity.class));
        verify(caseRetentionRepository).save(any());
    }

    @Test
    void closeCaseAndSetRetention_shouldNotCreateRetention_whenNoPendingRetention_andEventBeforeExistingCaseClosedTimestamp() {
        OffsetDateTime eventTime = OffsetDateTime.of(2024, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        courtCase.setCaseClosedTimestamp(eventTime.plusDays(1));

        DartsEventRetentionPolicy retentionPolicy = new DartsEventRetentionPolicy();
        retentionPolicy.setCaseRetentionFixedPolicy("OVERRIDABLE_POLICY");
        retentionPolicy.setCaseTotalSentence("P1Y");

        dartsEvent.setDateTime(eventTime);
        dartsEvent.setRetentionPolicy(retentionPolicy);

        when(caseRetentionRepository.findLatestPendingRetention(courtCase)).thenReturn(Optional.empty());

        service.closeCaseAndSetRetention(dartsEvent, hearingAndEvent, courtCase);
        
        verify(caseRepository).saveAndFlush(courtCase);

        // no retention should be created because the event time is before existing caseClosedTimestamp
        verify(caseRetentionRepository, never()).save(any(CaseRetentionEntity.class));
        verify(retentionApi, never()).applyPolicyStringToDate(any(), any(), any());
    }

    @Test
    void closeCaseAndSetRetention_shouldCreateRetention_whenNoPendingRetention_andEventAtOrAfterCaseClosedTimestamp() {
        OffsetDateTime eventTime = OffsetDateTime.of(2024, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        // ensure create path is taken (caseClosedTimestamp is null before closeCase sets it)
        courtCase.setCaseClosedTimestamp(null);

        DartsEventRetentionPolicy retentionPolicy = new DartsEventRetentionPolicy();
        retentionPolicy.setCaseRetentionFixedPolicy("OVERRIDABLE_POLICY");
        retentionPolicy.setCaseTotalSentence("P1Y");

        dartsEvent.setDateTime(eventTime);
        dartsEvent.setRetentionPolicy(retentionPolicy);

        when(caseRetentionRepository.findLatestPendingRetention(courtCase)).thenReturn(Optional.empty());

        LocalDate expectedRetainUntilDate = LocalDate.of(2030, 1, 1);
        when(retentionApi.applyPolicyStringToDate(eq(eventTime.toLocalDate()), eq("P1Y"), eq(retentionPolicyType)))
            .thenReturn(expectedRetainUntilDate);

        service.closeCaseAndSetRetention(dartsEvent, hearingAndEvent, courtCase);

        verify(caseRetentionRepository).save(caseRetentionCaptor.capture());
        CaseRetentionEntity saved = caseRetentionCaptor.getValue();

        assertThat(saved.getCourtCase()).isSameAs(courtCase);
        assertThat(saved.getRetentionPolicyType()).isSameAs(retentionPolicyType);
        assertThat(saved.getCaseManagementRetention()).isSameAs(caseManagementRetention);
        assertThat(saved.getTotalSentence()).isEqualTo("P1Y");
        assertThat(saved.getCurrentState()).isEqualTo(CaseRetentionStatus.PENDING.name());
        assertThat(saved.getConfidenceCategory()).isEqualTo(RetentionConfidenceCategoryEnum.CASE_CLOSED.getId());
        assertThat(saved.getRetainUntil())
            .isEqualTo(expectedRetainUntilDate.atStartOfDay().atOffset(ZoneOffset.UTC));

        // closeCase should set closed timestamp to event time when it was previously null
        assertThat(courtCase.getClosed()).isTrue();
        assertThat(courtCase.getCaseClosedTimestamp()).isEqualTo(eventTime);
    }

    @Test
    void closeCaseAndSetRetention_shouldUpdateExistingRetention_whenPendingRetentionExists_andEventAfterPendingTimestamp() {
        OffsetDateTime pendingEventTime = OffsetDateTime.of(2024, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime eventTime = pendingEventTime.plusHours(2);

        DartsEventRetentionPolicy retentionPolicy = new DartsEventRetentionPolicy();
        retentionPolicy.setCaseRetentionFixedPolicy("OVERRIDABLE_POLICY");
        retentionPolicy.setCaseTotalSentence("P2Y");

        dartsEvent.setDateTime(eventTime);
        dartsEvent.setRetentionPolicy(retentionPolicy);

        CaseRetentionEntity existingRetention = new CaseRetentionEntity();
        existingRetention.setCourtCase(courtCase);

        when(caseRetentionRepository.findLatestPendingRetention(courtCase)).thenReturn(Optional.of(pendingRetention));
        when(pendingRetention.getEventTimestamp()).thenReturn(pendingEventTime);
        when(pendingRetention.getCaseRetention()).thenReturn(existingRetention);

        LocalDate expectedRetainUntilDate = LocalDate.of(2031, 6, 15);
        when(retentionApi.applyPolicyStringToDate(eq(eventTime.toLocalDate()), eq("P2Y"), eq(retentionPolicyType)))
            .thenReturn(expectedRetainUntilDate);

        service.closeCaseAndSetRetention(dartsEvent, hearingAndEvent, courtCase);

        verify(caseRetentionRepository).save(caseRetentionCaptor.capture());
        CaseRetentionEntity saved = caseRetentionCaptor.getValue();

        assertThat(saved).isSameAs(existingRetention);
        assertThat(saved.getRetentionPolicyType()).isSameAs(retentionPolicyType);
        assertThat(saved.getCaseManagementRetention()).isSameAs(caseManagementRetention);
        assertThat(saved.getTotalSentence()).isEqualTo("P2Y");
        assertThat(saved.getConfidenceCategory()).isEqualTo(RetentionConfidenceCategoryEnum.CASE_CLOSED.getId());
        assertThat(saved.getRetainUntil())
            .isEqualTo(expectedRetainUntilDate.atStartOfDay().atOffset(ZoneOffset.UTC));
    }

    @Test
    void closeCaseAndSetRetention_shouldNotUpdateExistingRetention_whenPendingRetentionExists_andEventNotAfterPendingTimestamp() {
        OffsetDateTime pendingEventTime = OffsetDateTime.of(2024, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime eventTime = pendingEventTime; // equal => should be ignored

        DartsEventRetentionPolicy retentionPolicy = new DartsEventRetentionPolicy();
        retentionPolicy.setCaseRetentionFixedPolicy("OVERRIDABLE_POLICY");
        retentionPolicy.setCaseTotalSentence("P2Y");

        dartsEvent.setDateTime(eventTime);
        dartsEvent.setRetentionPolicy(retentionPolicy);

        CaseRetentionEntity existingRetention = new CaseRetentionEntity();
        existingRetention.setCourtCase(courtCase);

        when(caseRetentionRepository.findLatestPendingRetention(courtCase)).thenReturn(Optional.of(pendingRetention));
        when(pendingRetention.getEventTimestamp()).thenReturn(pendingEventTime);

        service.closeCaseAndSetRetention(dartsEvent, hearingAndEvent, courtCase);

        verify(caseRetentionRepository, never()).save(any(CaseRetentionEntity.class));
        verify(retentionApi, never()).applyPolicyStringToDate(any(), any(), any());
    }

    @Test
    void closeCaseAndSetRetention_shouldReturnEarly_whenCompletedManualRetentionExists() {
        OffsetDateTime eventTime = OffsetDateTime.of(2024, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);

        DartsEventRetentionPolicy retentionPolicy = new DartsEventRetentionPolicy();
        retentionPolicy.setCaseRetentionFixedPolicy("OVERRIDABLE_POLICY");
        retentionPolicy.setCaseTotalSentence("P1Y");

        dartsEvent.setDateTime(eventTime);
        dartsEvent.setRetentionPolicy(retentionPolicy);

        when(caseRetentionRepository.findLatestCompletedManualRetention(courtCase))
            .thenReturn(Optional.of(new CaseRetentionEntity()));

        service.closeCaseAndSetRetention(dartsEvent, hearingAndEvent, courtCase);

        // should still close the case
        verify(caseRepository).saveAndFlush(courtCase);

        // but should not attempt to create/update retention when manual retention exists
        verify(caseRetentionRepository, never()).findLatestPendingRetention(any());
        verify(caseRetentionRepository, never()).save(any(CaseRetentionEntity.class));
        verify(retentionApi, never()).applyPolicyStringToDate(any(), any(), any());
    }

}