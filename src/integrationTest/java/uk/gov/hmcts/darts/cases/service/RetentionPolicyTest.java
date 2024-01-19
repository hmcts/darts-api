package uk.gov.hmcts.darts.cases.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.cases.mapper.CasesMapper;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.retention.enums.RetentionPolicy;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.darts.testutils.data.HearingTestData.createSomeMinimalHearing;

@SuppressWarnings("VariableDeclarationUsageDistance")
class RetentionPolicyTest extends IntegrationBase {

    @Autowired
    private CasesMapper casesMapper;

    @Test
    void singlePolicyWithCurrentStateComplete() {
        var minimalHearing = createSomeMinimalHearing();
        CaseRetentionEntity caseRetention = createCompleteCaseRetention(minimalHearing.getCourtCase());

        List<CaseRetentionEntity> caseRetentionList = new ArrayList<>();
        caseRetentionList.add(caseRetention);
        minimalHearing.getCourtCase().setCaseRetentionEntities(caseRetentionList);

        var hearingEntity = dartsDatabase.saveRetentionsForHearing(minimalHearing, caseRetentionList);
        var singleCase = casesMapper.mapToSingleCase(hearingEntity.getCourtCase());

        Assertions.assertEquals(singleCase.getRetentionPolicyApplied(), caseRetention.getRetentionPolicyType().getPolicyName());
        Assertions.assertEquals(singleCase.getRetainUntilDateTime(), caseRetention.getRetainUntil());
        Assertions.assertEquals(singleCase.getRetentionDateTimeApplied(), caseRetention.getRetainUntilAppliedOn());
    }

    @Test
    void twoPoliciesWithCurrentStateCompleteReturnLatest() {
        var minimalHearing = createSomeMinimalHearing();
        CaseRetentionEntity oldCaseRetentionCompleted = createCompleteCaseRetention(minimalHearing.getCourtCase());
        CaseRetentionEntity latestCaseRetentionCompleted = createCompleteCaseRetention(minimalHearing.getCourtCase());
        latestCaseRetentionCompleted.setRetainUntil(OffsetDateTime.parse("2030-01-31T15:42:10.361Z"));
        latestCaseRetentionCompleted.setRetainUntilAppliedOn(OffsetDateTime.parse("2023-07-22T15:42:10.361Z"));

        List<CaseRetentionEntity> caseRetentionList = new ArrayList<>();
        caseRetentionList.add(oldCaseRetentionCompleted);
        caseRetentionList.add(latestCaseRetentionCompleted);
        minimalHearing.getCourtCase().setCaseRetentionEntities(caseRetentionList);

        var hearingEntity = dartsDatabase.saveRetentionsForHearing(minimalHearing, caseRetentionList);
        var singleCase = casesMapper.mapToSingleCase(hearingEntity.getCourtCase());

        Assertions.assertEquals(singleCase.getRetentionPolicyApplied(), latestCaseRetentionCompleted.getRetentionPolicyType().getPolicyName());
        Assertions.assertEquals(singleCase.getRetainUntilDateTime(), latestCaseRetentionCompleted.getRetainUntil());
        Assertions.assertEquals(singleCase.getRetentionDateTimeApplied(), latestCaseRetentionCompleted.getRetainUntilAppliedOn());
    }

    @Test
    void twoPoliciesWithCurrentStateCompleteAndPending() {
        var minimalHearing = createSomeMinimalHearing();

        CaseRetentionEntity olderCaseRetentionComplete = createCompleteCaseRetention(minimalHearing.getCourtCase());
        CaseRetentionEntity caseRetentionPending = createCompleteCaseRetention(minimalHearing.getCourtCase());
        caseRetentionPending.setCurrentState(String.valueOf(CaseRetentionStatus.PENDING));

        List<CaseRetentionEntity> caseRetentionList = new ArrayList<>();
        caseRetentionList.add(olderCaseRetentionComplete);
        caseRetentionList.add(caseRetentionPending);
        minimalHearing.getCourtCase().setCaseRetentionEntities(caseRetentionList);

        var hearingEntity = dartsDatabase.saveRetentionsForHearing(minimalHearing, caseRetentionList);
        var singleCase = casesMapper.mapToSingleCase(hearingEntity.getCourtCase());

        Assertions.assertEquals(singleCase.getRetentionPolicyApplied(), olderCaseRetentionComplete.getRetentionPolicyType().getPolicyName());
        Assertions.assertEquals(singleCase.getRetainUntilDateTime(), olderCaseRetentionComplete.getRetainUntil());
        Assertions.assertEquals(singleCase.getRetentionDateTimeApplied(), olderCaseRetentionComplete.getRetainUntilAppliedOn());
    }

    @Test
    void threePoliciesTwoWithCurrentStateCompleteAndOnePending() {
        var minimalHearing = createSomeMinimalHearing();

        CaseRetentionEntity olderCaseRetentionCompleted = createCompleteCaseRetention(minimalHearing.getCourtCase());
        CaseRetentionEntity latestCaseRetentionCompleted = createCompleteCaseRetention(minimalHearing.getCourtCase());
        latestCaseRetentionCompleted.setRetainUntil(OffsetDateTime.parse("2030-01-31T15:42:10.361Z"));
        latestCaseRetentionCompleted.setRetainUntilAppliedOn(OffsetDateTime.parse("2023-07-22T15:42:10.361Z"));
        CaseRetentionEntity caseRetentionPending = createCompleteCaseRetention(minimalHearing.getCourtCase());
        caseRetentionPending.setRetainUntil(OffsetDateTime.parse("2030-01-31T15:42:10.361Z"));
        caseRetentionPending.setRetainUntilAppliedOn(OffsetDateTime.parse("2023-07-22T15:42:10.361Z"));
        caseRetentionPending.setCurrentState(String.valueOf(CaseRetentionStatus.PENDING));

        List<CaseRetentionEntity> caseRetentionList = new ArrayList<>();
        caseRetentionList.add(olderCaseRetentionCompleted);
        caseRetentionList.add(latestCaseRetentionCompleted);
        caseRetentionList.add(caseRetentionPending);
        minimalHearing.getCourtCase().setCaseRetentionEntities(caseRetentionList);

        var hearingEntity = dartsDatabase.saveRetentionsForHearing(minimalHearing, caseRetentionList);
        var singleCase = casesMapper.mapToSingleCase(hearingEntity.getCourtCase());

        Assertions.assertEquals(singleCase.getRetentionPolicyApplied(), latestCaseRetentionCompleted.getRetentionPolicyType().getPolicyName());
        Assertions.assertEquals(singleCase.getRetainUntilDateTime(), latestCaseRetentionCompleted.getRetainUntil());
        Assertions.assertEquals(singleCase.getRetentionDateTimeApplied(), latestCaseRetentionCompleted.getRetainUntilAppliedOn());
    }

    @Test
    void noRentionPolicyAdded() {
        var minimalHearing = createSomeMinimalHearing();
        List<CaseRetentionEntity> caseRetentionList = new ArrayList<>();
        var hearingEntity = dartsDatabase.saveRetentionsForHearing(minimalHearing, caseRetentionList);
        var singleCase = casesMapper.mapToSingleCase(hearingEntity.getCourtCase());

        Assertions.assertNull(singleCase.getRetentionPolicyApplied());
        Assertions.assertNull(singleCase.getRetainUntilDateTime());
        Assertions.assertNull(singleCase.getRetentionDateTimeApplied());
    }

    @Test
    void onePolicyWithCurrentStatePending() {
        var minimalHearing = createSomeMinimalHearing();

        minimalHearing.getCourtCase().setCaseClosedTimestamp(OffsetDateTime.now());
        CaseRetentionEntity caseRetentionPending = createCompleteCaseRetention(minimalHearing.getCourtCase());
        caseRetentionPending.setCurrentState(String.valueOf(CaseRetentionStatus.PENDING));

        List<CaseRetentionEntity> caseRetentionList = new ArrayList<>();
        caseRetentionList.add(caseRetentionPending);
        minimalHearing.getCourtCase().setCaseRetentionEntities(caseRetentionList);

        var hearingEntity = dartsDatabase.saveRetentionsForHearing(minimalHearing, caseRetentionList);
        var singleCase = casesMapper.mapToSingleCase(hearingEntity.getCourtCase());

        Assertions.assertNull(singleCase.getRetentionPolicyApplied());
        Assertions.assertNull(singleCase.getRetainUntilDateTime());
        Assertions.assertNull(singleCase.getRetentionDateTimeApplied());
        Assertions.assertEquals(singleCase.getCaseClosedDateTime(), caseRetentionPending.getCourtCase().getCaseClosedTimestamp());

    }

    private static CaseRetentionEntity createCompleteCaseRetention(CourtCaseEntity courtCase) {
        UserAccountEntity testUser = new UserAccountEntity();
        CaseRetentionEntity caseRetention = new CaseRetentionEntity();
        caseRetention.setRetentionPolicyType(createRetentionPolicyType());
        caseRetention.setTotalSentence("20Y2M10D");
        caseRetention.setRetainUntil(OffsetDateTime.parse("2029-01-31T15:42:10.361Z"));
        caseRetention.setRetainUntilAppliedOn(OffsetDateTime.parse("2022-07-22T15:42:10.361Z"));
        caseRetention.setCurrentState(String.valueOf(CaseRetentionStatus.COMPLETE));
        caseRetention.setSubmittedBy(testUser);
        caseRetention.setLastModifiedBy(testUser);
        caseRetention.setCourtCase(courtCase);
        return caseRetention;
    }

    private static RetentionPolicyTypeEntity createRetentionPolicyType() {
        RetentionPolicyTypeEntity retentionPolicyType = new RetentionPolicyTypeEntity();
        retentionPolicyType.setFixedPolicyKey(1);
        retentionPolicyType.setPolicyName(String.valueOf(RetentionPolicy.STANDARD));
        retentionPolicyType.setPolicyStart(OffsetDateTime.parse("2023-07-22T15:42:10.361Z"));
        retentionPolicyType.setPolicyEnd(OffsetDateTime.parse("2030-01-31T15:42:10.361Z"));
        retentionPolicyType.setCreatedDateTime(OffsetDateTime.parse("2023-07-22T15:42:10.361Z"));
        retentionPolicyType.setDuration("1");
        return retentionPolicyType;
    }

}
