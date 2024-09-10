package uk.gov.hmcts.darts.cases.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.cases.service.CloseOldCasesProcessor;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.retention.api.RetentionApi;
import uk.gov.hmcts.darts.retention.helper.RetentionDateHelper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloseOldCasesProcessorImplTest {

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

    private UserAccountEntity userAccountEntity;

    private CloseOldCasesProcessor closeOldCasesProcessor;

    @BeforeEach
    void setUp() {
        userAccountEntity = CommonTestDataUtil.createUserAccountWithId();
        when(authorisationApi.getCurrentUser()).thenReturn(userAccountEntity);

        closeOldCasesProcessor = new CloseOldCasesProcessorImpl(caseRepository,
                                                                caseRetentionRepository,
                                                                retentionApi,
                                                                retentionDateHelper,
                                                                authorisationApi);
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
        when(caseRepository.findOpenCasesToClose(any(), any())).thenReturn(List.of(courtCase));

        CaseRetentionEntity caseRetention = createRetentionEntity(courtCase, userAccountEntity);
        when(retentionApi.createRetention(any(), any(), any(), any(), any())).thenReturn(caseRetention);
        assertFalse(courtCase.getClosed());

        // when
        closeOldCasesProcessor.closeCases(2);

        // then
        assertTrue(courtCase.getClosed());
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