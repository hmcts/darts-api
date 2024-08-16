package uk.gov.hmcts.darts.cases.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.CourtCaseStub;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.test.common.data.CaseTestData.createSomeMinimalCase;

class CaseRepositoryIntTest extends IntegrationBase {
    protected static final String SOME_COURTHOUSE = "some-courthouse";
    protected static final String SOME_ROOM = "some-room";
    protected static final String SOME_CASE_NUMBER_1 = "CASE1";
    protected static final String SOME_CASE_NUMBER_2 = "CASE2";

    private final OffsetDateTime testTime = OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    CourtCaseStub caseStub;

    @Autowired
    CaseRepository caseRepository;

    @Test
    void testFindByIsRetentionUpdatedTrueAndRetentionRetriesLessThan() {
        // TODO: use CaseTestData
        // given
        CourtCaseEntity caseWithRetries3 = createSomeMinimalCase();
        caseWithRetries3.setRetentionUpdated(true);
        caseWithRetries3.setRetentionRetries(3);
        dartsDatabase.save(caseWithRetries3);

        CourtCaseEntity caseWithRetries4 = createSomeMinimalCase();
        caseWithRetries4.setRetentionUpdated(true);
        caseWithRetries4.setRetentionRetries(4);
        dartsDatabase.save(caseWithRetries4);

        CourtCaseEntity caseWithNoUpdate = createSomeMinimalCase();
        caseWithNoUpdate.setRetentionUpdated(false);
        caseWithNoUpdate.setRetentionRetries(1);
        dartsDatabase.save(caseWithNoUpdate);

        CourtCaseEntity matchingCase = createSomeMinimalCase();
        matchingCase.setRetentionUpdated(true);
        matchingCase.setRetentionRetries(1);
        dartsDatabase.save(matchingCase);

        // when
        var result = caseRepository.findByIsRetentionUpdatedTrueAndRetentionRetriesLessThan(3);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(matchingCase.getId());
    }

    @Test
    void testFindCasesNeedingCaseDocumentGeneratedPaged() {
        // given
        CourtCaseEntity caseClosed27DaysAgo = createSomeMinimalCase();
        caseClosed27DaysAgo.setClosed(true);
        caseClosed27DaysAgo.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(27));
        dartsDatabase.save(caseClosed27DaysAgo);

        CourtCaseEntity matchingCase1 = createSomeMinimalCase();
        matchingCase1.setClosed(true);
        matchingCase1.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(28));
        dartsDatabase.save(matchingCase1);

        CourtCaseEntity matchingCase2 = createSomeMinimalCase();
        matchingCase2.setClosed(true);
        matchingCase2.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(29));
        dartsDatabase.save(matchingCase2);

        CourtCaseEntity matchingCase3 = createSomeMinimalCase();
        matchingCase3.setClosed(true);
        matchingCase3.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(29));
        dartsDatabase.save(matchingCase3);

        CourtCaseEntity courtCaseWithCaseDocument = createSomeMinimalCase();
        courtCaseWithCaseDocument.setClosed(true);
        courtCaseWithCaseDocument.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(29));
        dartsDatabase.save(courtCaseWithCaseDocument);
        dartsDatabase.getCaseDocumentStub().createCaseDocumentEntity(courtCaseWithCaseDocument, courtCaseWithCaseDocument.getCreatedBy());

        CourtCaseEntity openCase = createSomeMinimalCase();
        dartsDatabase.save(openCase);

        // when
        List<CourtCaseEntity> result = caseRepository.findCasesNeedingCaseDocumentGenerated(
            OffsetDateTime.now().minusDays(28), Pageable.ofSize(2));

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(matchingCase1.getId());
        assertThat(result.get(1).getId()).isEqualTo(matchingCase2.getId());
    }

    @Test
    void testFindCasesNeedingCaseDocumentGeneratedUnpaged() {
        // given
        CourtCaseEntity notMatchingCase = createSomeMinimalCase();
        notMatchingCase.setClosed(true);
        notMatchingCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(27));
        dartsDatabase.save(notMatchingCase);

        CourtCaseEntity courtCaseWithCaseDocument = createSomeMinimalCase();
        courtCaseWithCaseDocument.setClosed(true);
        courtCaseWithCaseDocument.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(29));
        dartsDatabase.save(courtCaseWithCaseDocument);
        dartsDatabase.getCaseDocumentStub().createAndSaveCaseDocumentEntity(courtCaseWithCaseDocument);

        CourtCaseEntity openCase = createSomeMinimalCase();
        dartsDatabase.save(openCase);

        CourtCaseEntity matchingCase1 = createSomeMinimalCase();
        matchingCase1.setClosed(true);
        matchingCase1.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(28));
        dartsDatabase.save(matchingCase1);

        CourtCaseEntity matchingCase2 = createSomeMinimalCase();
        matchingCase2.setClosed(true);
        matchingCase2.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(28));
        dartsDatabase.save(matchingCase2);

        CourtCaseEntity matchingCase3 = createSomeMinimalCase();
        matchingCase3.setClosed(true);
        matchingCase3.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(29));
        dartsDatabase.save(matchingCase3);

        assertThat(dartsDatabase.getCaseRepository().findAll()).hasSize(6);

        // when
        List<CourtCaseEntity> result = caseRepository.findCasesNeedingCaseDocumentGenerated(
            OffsetDateTime.now().minusDays(28), Pageable.unpaged());

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getId()).isEqualTo(matchingCase1.getId());
        assertThat(result.get(1).getId()).isEqualTo(matchingCase2.getId());
        assertThat(result.get(2).getId()).isEqualTo(matchingCase3.getId());
    }


    @Test
    void testFindCasesNeedingCaseDocumentForRetentionDateGenerationPagedSuccess() {
        // given
        CourtCaseEntity courtCaseEntityWithNoCaseDocuments = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER_1);

        dartsDatabase.getHearingStub().createHearing(SOME_COURTHOUSE, SOME_ROOM, SOME_CASE_NUMBER_1,
                                                     DateConverterUtil.toLocalDateTime(testTime));

        CaseRetentionEntity caseRetentionObject1 = dartsDatabase.createCaseRetentionObject(
            courtCaseEntityWithNoCaseDocuments, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(30), false);
        dartsDatabase.save(caseRetentionObject1);

        CaseRetentionEntity caseRetentionObject2 = dartsDatabase.createCaseRetentionObject(
            courtCaseEntityWithNoCaseDocuments, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(20), false);
        dartsDatabase.save(caseRetentionObject2);

        CourtCaseEntity courtCaseEntityWithCaseDocument = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER_2);

        dartsDatabase.getHearingStub().createHearing(SOME_COURTHOUSE, SOME_ROOM, SOME_CASE_NUMBER_2,
                                                     DateConverterUtil.toLocalDateTime(testTime));

        CaseRetentionEntity caseRetentionObject3 = dartsDatabase.createCaseRetentionObject(
            courtCaseEntityWithCaseDocument, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(10), false);
        dartsDatabase.save(caseRetentionObject3);

        UserAccountEntity uploadedBy = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        CaseDocumentEntity caseDocument = dartsDatabase.getCaseDocumentStub().createAndSaveCaseDocumentEntity(
            courtCaseEntityWithCaseDocument, uploadedBy);
        caseDocument.setCreatedDateTime(OffsetDateTime.now().minusDays(30));
        dartsDatabase.save(caseDocument);

        OffsetDateTime currentTimestamp = OffsetDateTime.now();

        // when
        List<CourtCaseEntity> result = caseRepository.findCasesNeedingCaseDocumentForRetentionDateGeneration(
            currentTimestamp.plusDays(28), currentTimestamp.minusDays(28), Pageable.ofSize(2));

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(courtCaseEntityWithNoCaseDocuments.getId());
        assertThat(result.get(1).getId()).isEqualTo(courtCaseEntityWithCaseDocument.getId());

    }

    @Test
    void testFindCasesNeedingCaseDocumentForRetentionDateGenerationPagedWhereRetentionDateToFarInTheFuture() {
        // given
        CourtCaseEntity courtCaseEntityWithNoCaseDocuments = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER_1);

        dartsDatabase.getHearingStub().createHearing(SOME_COURTHOUSE, SOME_ROOM, SOME_CASE_NUMBER_1,
                                                     DateConverterUtil.toLocalDateTime(testTime));

        CaseRetentionEntity caseRetentionObject1 = dartsDatabase.createCaseRetentionObject(
            courtCaseEntityWithNoCaseDocuments, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(30), false);
        dartsDatabase.save(caseRetentionObject1);

        CourtCaseEntity courtCaseEntityWithCaseDocument = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER_2);

        dartsDatabase.getHearingStub().createHearing(SOME_COURTHOUSE, SOME_ROOM, SOME_CASE_NUMBER_2,
                                                     DateConverterUtil.toLocalDateTime(testTime));

        CaseRetentionEntity caseRetentionObject3 = dartsDatabase.createCaseRetentionObject(
            courtCaseEntityWithCaseDocument, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(40), false);
        dartsDatabase.save(caseRetentionObject3);

        UserAccountEntity uploadedBy = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        CaseDocumentEntity caseDocument = dartsDatabase.getCaseDocumentStub().createAndSaveCaseDocumentEntity(courtCaseEntityWithCaseDocument, uploadedBy);
        caseDocument.setCreatedDateTime(OffsetDateTime.now().minusDays(30));
        dartsDatabase.save(caseDocument);

        OffsetDateTime currentTimestamp = OffsetDateTime.now();

        // when
        List<CourtCaseEntity> result = caseRepository.findCasesNeedingCaseDocumentForRetentionDateGeneration(
            currentTimestamp.plusDays(28), currentTimestamp.minusDays(28), Pageable.ofSize(2));

        // then
        assertThat(result).hasSize(0);

    }

    @Test
    void testFindCasesNeedingCaseDocumentForRetentionDateGenerationPagedWithRecentDocuments() {
        // given
        CourtCaseEntity courtCaseEntityWithCaseDocuments1 = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER_1);

        dartsDatabase.getHearingStub().createHearing(SOME_COURTHOUSE, SOME_ROOM, SOME_CASE_NUMBER_1,
                                                     DateConverterUtil.toLocalDateTime(testTime));

        CaseRetentionEntity caseRetentionObject1 = dartsDatabase.createCaseRetentionObject(
            courtCaseEntityWithCaseDocuments1, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(30), false);
        dartsDatabase.save(caseRetentionObject1);

        CaseRetentionEntity caseRetentionObject2 = dartsDatabase.createCaseRetentionObject(
            courtCaseEntityWithCaseDocuments1, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(20), false);
        dartsDatabase.save(caseRetentionObject2);

        UserAccountEntity uploadedBy = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        CaseDocumentEntity caseDocument1 = dartsDatabase.getCaseDocumentStub().createAndSaveCaseDocumentEntity(
            courtCaseEntityWithCaseDocuments1, uploadedBy);
        caseDocument1.setCreatedDateTime(OffsetDateTime.now().minusDays(27));
        dartsDatabase.save(caseDocument1);

        CourtCaseEntity courtCaseEntityWithCaseDocument2 = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER_2);

        dartsDatabase.getHearingStub().createHearing(SOME_COURTHOUSE, SOME_ROOM, SOME_CASE_NUMBER_2,
                                                     DateConverterUtil.toLocalDateTime(testTime));

        CaseRetentionEntity caseRetentionObject3 = dartsDatabase.createCaseRetentionObject(
            courtCaseEntityWithCaseDocument2, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(10), false);
        dartsDatabase.save(caseRetentionObject3);

        CaseDocumentEntity caseDocument = dartsDatabase.getCaseDocumentStub().createAndSaveCaseDocumentEntity(
            courtCaseEntityWithCaseDocument2, uploadedBy);
        dartsDatabase.save(caseDocument);

        OffsetDateTime currentTimestamp = OffsetDateTime.now();

        // when
        List<CourtCaseEntity> result = caseRepository.findCasesNeedingCaseDocumentForRetentionDateGeneration(
            currentTimestamp.plusDays(28), currentTimestamp.minusDays(28), Pageable.ofSize(2));

        // then
        assertThat(result).hasSize(0);

    }
}
