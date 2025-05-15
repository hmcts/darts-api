package uk.gov.hmcts.darts.retention.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.event.model.CreatedHearingAndEvent;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.model.DartsEventRetentionPolicy;
import uk.gov.hmcts.darts.event.service.CaseManagementRetentionService;
import uk.gov.hmcts.darts.retention.api.RetentionApi;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceCategoryEnum;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloseCaseWithRetentionServiceImplTest {

    @Mock
    private CaseRetentionRepository caseRetentionRepository;

    @Mock
    private CaseManagementRetentionService caseManagementRetentionService;

    @Mock
    private RetentionApi retentionApi;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private AuthorisationApi authorisationApi;

    @InjectMocks
    private CloseCaseWithRetentionServiceImpl service;

    // Note, the main test scenarios for this class are covered in the integration StopAndCloseHandlerTest
    @Test
    void closeCaseAndSetRetention_shouldCloseAndSetRetentionOnCase() {
        DartsEvent dartsEvent = new DartsEvent();
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
        when(retentionApi.updateCourtCaseConfidenceAttributesForRetention(courtCase, RetentionConfidenceCategoryEnum.CASE_CLOSED))
            .thenReturn(courtCase);
        when(authorisationApi.getCurrentUser()).thenReturn(mock(UserAccountEntity.class));

        service.closeCaseAndSetRetention(dartsEvent, hearingAndEvent, courtCase);

        verify(caseRepository).saveAndFlush(any(CourtCaseEntity.class));
        verify(caseRetentionRepository).save(any());
    }
}