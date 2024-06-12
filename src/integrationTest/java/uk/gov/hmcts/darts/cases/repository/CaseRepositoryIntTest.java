package uk.gov.hmcts.darts.cases.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.CourtCaseStub;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CaseRepositoryIntTest extends IntegrationBase {

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
        var result = caseRepository.findByIsRetentionUpdatedTrueAndRetentionRetriesLessThan(3);

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

        assertThat(dartsDatabase.getCaseRepository().findAll()).hasSize(6);

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

        var matchingCase3 = caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(true);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(29));
        });

        var courtCaseWithCaseDocument = caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(true);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(29));
        });
        dartsDatabase.getCaseDocumentStub().createAndSaveCaseDocumentEntity(courtCaseWithCaseDocument);

        caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(false);
        });

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
}
