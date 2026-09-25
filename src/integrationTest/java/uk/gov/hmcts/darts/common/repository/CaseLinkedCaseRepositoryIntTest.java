package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CaseLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CaseLinkedCaseRepositoryIntTest extends PostgresIntegrationBase {

    private static final String CASE_NUMBER_1 = "CASE-1";
    private static final String CASE_NUMBER_2 = "CASE-2";

    @Autowired
    private CaseLinkedCaseRepository caseLinkedCaseRepository;

    @Test
    void findByCourtCase_shouldReturnLinkedCase_whenCaseIsLinkedOnEitherSide() {
        // given
        CourtCaseEntity courtCase1 = createCourtCase(CASE_NUMBER_1);
        CourtCaseEntity courtCase2 = createCourtCase(CASE_NUMBER_2);

        CaseLinkedCaseEntity linkedCase = createLinkedCase(courtCase1, courtCase2);
        clearEntityManagerCache();

        // when
        List<CaseLinkedCaseEntity> linkedCasesForCase1 = caseLinkedCaseRepository.findByCourtCase(courtCase1);
        List<CaseLinkedCaseEntity> linkedCasesForCase2 = caseLinkedCaseRepository.findByCourtCase(courtCase2);

        // then
        assertThat(linkedCasesForCase1)
            .extracting(CaseLinkedCaseEntity::getId)
            .containsExactly(linkedCase.getId());
        assertThat(linkedCasesForCase2)
            .extracting(CaseLinkedCaseEntity::getId)
            .containsExactly(linkedCase.getId());
    }

    @Test
    void findByCourtCaseIdIn_shouldReturnLinkedCasesForAllMatchingCaseIds() {
        // given
        CourtCaseEntity courtCase1 = createCourtCase(CASE_NUMBER_1);
        CourtCaseEntity courtCase2 = createCourtCase(CASE_NUMBER_2);
        CourtCaseEntity courtCase3 = createCourtCase("CASE-3");
        CourtCaseEntity courtCase4 = createCourtCase("CASE-4");
        CourtCaseEntity courtCase5 = createCourtCase("CASE-5");

        CaseLinkedCaseEntity linkedCase1 = createLinkedCase(courtCase1, courtCase2);
        CaseLinkedCaseEntity linkedCase2 = createLinkedCase(courtCase3, courtCase1);
        CaseLinkedCaseEntity linkedCase3 = createLinkedCase(courtCase4, courtCase5);
        clearEntityManagerCache();

        // when
        List<CaseLinkedCaseEntity> linkedCases = caseLinkedCaseRepository.findByCourtCaseIdIn(List.of(
            courtCase1.getId(),
            courtCase2.getId(),
            courtCase3.getId()
        ));

        // then
        assertThat(linkedCases)
            .extracting(CaseLinkedCaseEntity::getId)
            .containsExactlyInAnyOrder(linkedCase1.getId(), linkedCase2.getId())
            .doesNotContain(linkedCase3.getId());
    }

    private CourtCaseEntity createCourtCase(String caseNumber) {
        CourtCaseEntity courtCase = PersistableFactory.getCourtCaseTestData().caseWithCaseNumber(caseNumber);
        return dartsPersistence.save(courtCase);
    }

    private CaseLinkedCaseEntity createLinkedCase(CourtCaseEntity courtCase1, CourtCaseEntity courtCase2) {
        CaseLinkedCaseEntity linkedCase = new CaseLinkedCaseEntity();
        linkedCase.setCourtCase1(courtCase1);
        linkedCase.setCourtCase2(courtCase2);
        linkedCase.setCreatedById(0);
        linkedCase.setLastModifiedById(0);
        return caseLinkedCaseRepository.saveAndFlush(linkedCase);
    }
}
