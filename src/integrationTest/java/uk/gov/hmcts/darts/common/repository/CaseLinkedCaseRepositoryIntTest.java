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

        CaseLinkedCaseEntity linkedCase = new CaseLinkedCaseEntity();
        linkedCase.setCourtCase1(courtCase1);
        linkedCase.setCourtCase2(courtCase2);
        linkedCase.setCreatedById(0);
        linkedCase.setLastModifiedById(0);
        linkedCase = caseLinkedCaseRepository.saveAndFlush(linkedCase);
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

    private CourtCaseEntity createCourtCase(String caseNumber) {
        CourtCaseEntity courtCase = PersistableFactory.getCourtCaseTestData().caseWithCaseNumber(caseNumber);
        return dartsPersistence.save(courtCase);
    }
}

