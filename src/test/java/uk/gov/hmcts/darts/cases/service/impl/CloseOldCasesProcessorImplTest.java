package uk.gov.hmcts.darts.cases.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.cases.service.CloseOldCasesProcessor;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.retention.api.RetentionApi;
import uk.gov.hmcts.darts.retention.helper.RetentionDateHelper;
import uk.gov.hmcts.darts.task.config.CloseOldCasesAutomatedTaskConfig;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloseOldCasesProcessorImplTest {
    public static final OffsetDateTime CURRENT_DATE_TIME = OffsetDateTime.of(2024, 10, 1, 10, 0, 0, 0, ZoneOffset.UTC);

    @Mock
    private CaseRepository caseRepository;
    @Mock
    private CaseRetentionRepository caseRetentionRepository;
    @Mock
    private RetentionApi retentionApi;
    @Mock
    private RetentionDateHelper retentionDateHelper;
    @Mock
    private AuthorisationApi authorisationApi;

    @Mock
    private CurrentTimeHelper currentTimeHelper;
    @Mock
    private CaseService caseService;
    @Mock
    private CloseOldCasesAutomatedTaskConfig closeOldCasesAutomatedTaskConfig;

    private UserAccountEntity userAccountEntity;


    private CloseOldCasesProcessor closeOldCasesProcessor;

    @BeforeEach
    void setUp() {
        userAccountEntity = CommonTestDataUtil.createUserAccountWithId();
        when(authorisationApi.getCurrentUser()).thenReturn(userAccountEntity);
        CloseOldCasesProcessorImpl.CloseCaseProcessor caseProcessor = new CloseOldCasesProcessorImpl.CloseCaseProcessor(
            caseService,
            caseRetentionRepository,
            retentionApi,
            retentionDateHelper,
            currentTimeHelper,
            closeOldCasesAutomatedTaskConfig
        );

        closeOldCasesProcessor = new CloseOldCasesProcessorImpl(caseProcessor, caseRepository, authorisationApi, closeOldCasesAutomatedTaskConfig);

        lenient().when(currentTimeHelper.currentOffsetDateTime()).thenReturn(CURRENT_DATE_TIME);
        lenient().when(closeOldCasesAutomatedTaskConfig.getThreads()).thenReturn(10);
    }

    @Test
    void closeCases() {
        // given
        LocalDateTime hearingDate = DateConverterUtil.toLocalDateTime(OffsetDateTime.now().minusYears(7));
        OffsetDateTime createdDate = OffsetDateTime.now().minusYears(7);

        List<HearingEntity> hearings = CommonTestDataUtil.createHearings(1);
        HearingEntity hearingEntity = hearings.getFirst();
        hearingEntity.setHearingDate(hearingDate.minusDays(10).toLocalDate());
        CourtCaseEntity courtCase = hearingEntity.getCourtCase();
        courtCase.setCreatedDateTime(createdDate);
        courtCase.setClosed(false);
        courtCase.setHearings(hearings);
        courtCase.setId(1);
        when(caseRepository.findOpenCasesToClose(any(), any())).thenReturn(List.of(1));
        when(caseService.getCourtCaseById(1)).thenReturn(courtCase);

        CaseRetentionEntity caseRetention = createRetentionEntity(courtCase, userAccountEntity);
        when(retentionApi.createRetention(any(), any(), any(), any(), any())).thenReturn(caseRetention);
        assertFalse(courtCase.getClosed());
        assertNull(courtCase.getRetConfUpdatedTs());

        // when
        closeOldCasesProcessor.closeCases(2);

        // then
        assertTrue(courtCase.getClosed());
        assertEquals(CURRENT_DATE_TIME, courtCase.getRetConfUpdatedTs());
    }

    public static CaseRetentionEntity createRetentionEntity(CourtCaseEntity courtCase, UserAccountEntity userAccount) {
        CaseRetentionEntity caseRetention = new CaseRetentionEntity();
        caseRetention.setCourtCase(courtCase);
        caseRetention.setLastModifiedBy(userAccount);
        caseRetention.setCreatedBy(userAccount);
        caseRetention.setSubmittedBy(userAccount);
        return caseRetention;
    }
}