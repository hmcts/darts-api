package uk.gov.hmcts.darts.cases.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.cases.mapper.CasesMapper;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;

import static org.mockito.Mockito.when;

@SuppressWarnings("VariableDeclarationUsageDistance")
class RetentionPolicyTest extends IntegrationBase {

    @Autowired
    private CasesMapper casesMapper;

    @MockitoBean
    CurrentTimeHelper currentTimeHelper;

    @BeforeEach
    void setup() {
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.parse("2024-01-01T10:00:00.000Z"));
    }

    @Test
    void singlePolicyWithCurrentStateComplete() {
        CourtCaseEntity courtCase = dartsDatabase.createCase("Swansea", "aCaseNumber");
        createCompleteCaseRetention(courtCase);

        var singleCase = casesMapper.mapToSingleCase(courtCase);

        Assertions.assertEquals("DARTS Default Policy", singleCase.getRetentionPolicyApplied());
        Assertions.assertEquals(OffsetDateTime.parse("2029-01-31T15:42:10.361Z"), singleCase.getRetainUntilDateTime());
        Assertions.assertEquals(OffsetDateTime.parse("2024-01-01T10:00:00.000Z"), singleCase.getRetentionDateTimeApplied());
    }

    @Test
    void twoPoliciesWithCurrentStateCompleteReturnLatest() {
        CourtCaseEntity courtCase = dartsDatabase.createCase("Swansea", "aCaseNumber");
        createCompleteCaseRetention(courtCase);
        CaseRetentionEntity latestCaseRetentionCompleted = createCompleteCaseRetention(courtCase);
        latestCaseRetentionCompleted.setRetainUntil(OffsetDateTime.parse("2030-01-31T15:42:10.361Z"));
        latestCaseRetentionCompleted.setRetainUntilAppliedOn(OffsetDateTime.parse("2023-07-22T15:42:10.361Z"));
        dartsDatabase.save(latestCaseRetentionCompleted);

        var singleCase = casesMapper.mapToSingleCase(courtCase);

        Assertions.assertEquals("DARTS Default Policy", singleCase.getRetentionPolicyApplied());
        Assertions.assertEquals(OffsetDateTime.parse("2030-01-31T15:42:10.361Z"), singleCase.getRetainUntilDateTime());
        Assertions.assertEquals(OffsetDateTime.parse("2023-07-22T15:42:10.361Z"), singleCase.getRetentionDateTimeApplied());
    }

    @Test
    void twoPoliciesWithCurrentStateCompleteAndPending() {
        CourtCaseEntity courtCase = dartsDatabase.createCase("Swansea", "aCaseNumber");

        createCompleteCaseRetention(courtCase);
        CaseRetentionEntity caseRetentionPending = createCompleteCaseRetention(courtCase);
        caseRetentionPending.setCurrentState(String.valueOf(CaseRetentionStatus.PENDING));
        dartsDatabase.save(caseRetentionPending);

        var singleCase = casesMapper.mapToSingleCase(courtCase);

        Assertions.assertEquals("DARTS Default Policy", singleCase.getRetentionPolicyApplied());
        Assertions.assertEquals(OffsetDateTime.parse("2029-01-31T15:42:10.361Z"), singleCase.getRetainUntilDateTime());
        Assertions.assertEquals(OffsetDateTime.parse("2024-01-01T10:00:00.000Z"), singleCase.getRetentionDateTimeApplied());
    }

    @Test
    void threePoliciesTwoWithCurrentStateCompleteAndOnePending() {
        CourtCaseEntity courtCase = dartsDatabase.createCase("Swansea", "aCaseNumber");

        createCompleteCaseRetention(courtCase);
        CaseRetentionEntity latestCaseRetentionCompleted = createCompleteCaseRetention(courtCase);
        latestCaseRetentionCompleted.setRetainUntil(OffsetDateTime.parse("2030-01-31T15:42:10.361Z"));
        latestCaseRetentionCompleted.setRetainUntilAppliedOn(OffsetDateTime.parse("2023-07-22T15:42:10.361Z"));
        dartsDatabase.save(latestCaseRetentionCompleted);
        CaseRetentionEntity caseRetentionPending = createCompleteCaseRetention(courtCase);
        caseRetentionPending.setRetainUntil(OffsetDateTime.parse("2030-01-31T15:42:10.361Z"));
        caseRetentionPending.setRetainUntilAppliedOn(OffsetDateTime.parse("2023-07-22T15:42:10.361Z"));
        caseRetentionPending.setCurrentState(String.valueOf(CaseRetentionStatus.PENDING));
        dartsDatabase.save(caseRetentionPending);

        var singleCase = casesMapper.mapToSingleCase(courtCase);

        Assertions.assertEquals("DARTS Default Policy", singleCase.getRetentionPolicyApplied());
        Assertions.assertEquals(OffsetDateTime.parse("2030-01-31T15:42:10.361Z"), singleCase.getRetainUntilDateTime());
        Assertions.assertEquals(OffsetDateTime.parse("2023-07-22T15:42:10.361Z"), singleCase.getRetentionDateTimeApplied());
    }

    @Test
    void noRentionPolicyAdded() {
        CourtCaseEntity courtCase = dartsDatabase.createCase("Swansea", "aCaseNumber");
        var singleCase = casesMapper.mapToSingleCase(courtCase);

        Assertions.assertNull(singleCase.getRetentionPolicyApplied());
        Assertions.assertNull(singleCase.getRetainUntilDateTime());
        Assertions.assertNull(singleCase.getRetentionDateTimeApplied());
    }

    @Test
    void onePolicyWithCurrentStatePending() {
        CourtCaseEntity courtCase = dartsDatabase.createCase("Swansea", "aCaseNumber");
        courtCase.setCaseClosedTimestamp(OffsetDateTime.now());
        dartsDatabase.save(courtCase);

        CaseRetentionEntity caseRetentionPending = createCompleteCaseRetention(courtCase);
        caseRetentionPending.setCurrentState(String.valueOf(CaseRetentionStatus.PENDING));
        dartsDatabase.save(caseRetentionPending);

        var singleCase = casesMapper.mapToSingleCase(courtCase);

        Assertions.assertNull(singleCase.getRetentionPolicyApplied());
        Assertions.assertNull(singleCase.getRetainUntilDateTime());
        Assertions.assertNull(singleCase.getRetentionDateTimeApplied());
        Assertions.assertEquals(singleCase.getCaseClosedDateTime(), caseRetentionPending.getCourtCase().getCaseClosedTimestamp());

    }

    private CaseRetentionEntity createCompleteCaseRetention(CourtCaseEntity courtCase) {
        return dartsDatabase.createCaseRetentionObject(courtCase, CaseRetentionStatus.COMPLETE, OffsetDateTime.parse("2029-01-31T15:42:10.361Z"), false);
    }


}
