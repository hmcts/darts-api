package uk.gov.hmcts.darts.cases.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class CaseRepositoryIntTest extends IntegrationBase {
    protected static final String SOME_COURTHOUSE = "SOME-COURTHOUSE";
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
        // given
        caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setRetentionUpdated(true);
            courtCase.setRetentionRetries(3);
        });
        caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setRetentionUpdated(true);
            courtCase.setRetentionRetries(4);
        });
        caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setRetentionUpdated(false);
            courtCase.setRetentionRetries(1);
        });
        var matchingCase = caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setRetentionUpdated(true);
            courtCase.setRetentionRetries(1);
        });

        // when
        var result = caseRepository.findByIsRetentionUpdatedTrueAndRetentionRetriesLessThan(3, Limit.of(1000));

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(matchingCase.getId());
    }

    @Test
    void testFindCasesNeedingCaseDocumentGeneratedPaged() {
        // given
        caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(true);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(27));
        });

        var matchingCase1 = caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(true);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(28));
        });

        var matchingCase2 = caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(true);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(29));
        });

        caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(true);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(29));
        });

        var courtCaseWithCaseDocument = caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(true);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(29));
        });
        dartsDatabase.getCaseDocumentStub().createCaseDocumentEntity(courtCaseWithCaseDocument, courtCaseWithCaseDocument.getCreatedBy());

        caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(false);
        });

        // when
        List<CourtCaseEntity> result = caseRepository.findCasesNeedingCaseDocumentGenerated(
            OffsetDateTime.now().minusDays(28), Limit.of(2));

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(matchingCase1.getId());
        assertThat(result.get(1).getId()).isEqualTo(matchingCase2.getId());
    }

    @Test
    void testFindCasesNeedingCaseDocumentGeneratedUnpaged() {
        // given
        caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(true);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(27));
        });

        var courtCaseWithCaseDocument = caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(true);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(29));
        });
        dartsDatabase.getCaseDocumentStub().createAndSaveCaseDocumentEntity(courtCaseWithCaseDocument);

        caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(false);
        });

        var matchingCase1 = caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(true);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(28));
        });

        var matchingCase2 = caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(true);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(29));
        });

        var matchingCase3 = caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(true);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(29));
        });

        assertThat(dartsDatabase.getCaseRepository().findAll()).hasSize(6);

        // when
        List<CourtCaseEntity> result = caseRepository.findCasesNeedingCaseDocumentGenerated(
            OffsetDateTime.now().minusDays(28), Limit.unlimited());

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getId()).isEqualTo(matchingCase1.getId());
        assertThat(result.get(1).getId()).isEqualTo(matchingCase2.getId());
        assertThat(result.get(2).getId()).isEqualTo(matchingCase3.getId());
    }


    @Test
    void testFindCasesNeedingCaseDocumentForRetentionDateGenerationPagedSuccess() {
        // given

        AtomicInteger suffix = new AtomicInteger(1);
        Function<Boolean, CourtCaseEntity> createValidCourtCase = (isRetentionUpdated) -> {
            String caseNumber = "CASE" + suffix.getAndIncrement();
            CourtCaseEntity courtCase = dartsDatabase.createCase(SOME_COURTHOUSE, caseNumber);

            dartsDatabase.getHearingStub().createHearing(SOME_COURTHOUSE, SOME_ROOM, caseNumber,
                                                         DateConverterUtil.toLocalDateTime(testTime));

            CaseRetentionEntity caseRetentionObject1 = dartsDatabase.createCaseRetentionObject(
                courtCase, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(30), false);

            dartsDatabase.save(caseRetentionObject1);

            CaseRetentionEntity caseRetentionObject2 = dartsDatabase.createCaseRetentionObject(
                courtCase, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(20), false);
            dartsDatabase.save(caseRetentionObject2);
            courtCase.setRetentionUpdated(isRetentionUpdated);
            return dartsDatabase.save(courtCase);
        };

        CourtCaseEntity courtCase1 = createValidCourtCase.apply(true);
        CourtCaseEntity courtCase2 = createValidCourtCase.apply(false);
        CourtCaseEntity courtCase3 = createValidCourtCase.apply(true);
        CourtCaseEntity courtCase4 = createValidCourtCase.apply(false);


        OffsetDateTime currentTimestamp = OffsetDateTime.now();
        // when
        List<Integer> result = caseRepository.findCasesNeedingCaseDocumentForRetentionDateGeneration(
            currentTimestamp.plusDays(28), currentTimestamp.minusDays(28), Pageable.ofSize(4));

        // then
        assertThat(result).hasSize(4);
        assertThat(result.get(0)).isEqualTo(courtCase2.getId());
        assertThat(result.get(1)).isEqualTo(courtCase4.getId());
        assertThat(result.get(2)).isEqualTo(courtCase1.getId());
        assertThat(result.get(3)).isEqualTo(courtCase3.getId());

    }

    @Test
    void returnIsRetentionUpdatedFirst() {
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
        List<Integer> result = caseRepository.findCasesNeedingCaseDocumentForRetentionDateGeneration(
            currentTimestamp.plusDays(28), currentTimestamp.minusDays(28), Limit.of(2));

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(courtCaseEntityWithNoCaseDocuments.getId());
        assertThat(result.get(1)).isEqualTo(courtCaseEntityWithCaseDocument.getId());

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
        List<Integer> result = caseRepository.findCasesNeedingCaseDocumentForRetentionDateGeneration(
            currentTimestamp.plusDays(28), currentTimestamp.minusDays(28), Limit.of(2));

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
        List<Integer> result = caseRepository.findCasesNeedingCaseDocumentForRetentionDateGeneration(
            currentTimestamp.plusDays(28), currentTimestamp.minusDays(28), Limit.of(2));

        // then
        assertThat(result).hasSize(0);

    }

    @Test
    void testFindOpenCasesToClosePaged() {
        // given
        caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(false);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(27));
        });

        caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(true);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(27));
        });

        caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(false);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(30));
        });

        var foundCourtCase1 = caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(true);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(30));
        });

        // when
        List<CourtCaseEntity> result = caseRepository.findCasesNeedingCaseDocumentGenerated(
            OffsetDateTime.now().minusDays(28), Limit.of(2));

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(foundCourtCase1.getId());
    }
}