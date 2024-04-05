package uk.gov.hmcts.darts.cases.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.data.CaseTestData;

import static org.assertj.core.api.Assertions.assertThat;

class CaseRepositoryIntTest extends IntegrationBase {

    @Autowired
    CaseRepository caseRepository;

    @Test
    void testFindByIsRetentionUpdatedTrueAndRetentionRetriesLessThan() {
        // given
        var case1 = CaseTestData.createSomeMinimalCase();
        case1.setRetentionUpdated(true);
        case1.setRetentionRetries(1);
        caseRepository.save(case1);

        var case2 = CaseTestData.createSomeMinimalCase();
        case2.setRetentionUpdated(true);
        case2.setRetentionRetries(3);
        caseRepository.save(case2);

        var case3 = CaseTestData.createSomeMinimalCase();
        case3.setRetentionUpdated(true);
        case3.setRetentionRetries(4);
        caseRepository.save(case3);

        var case4 = CaseTestData.createSomeMinimalCase();
        case4.setRetentionUpdated(false);
        case4.setRetentionRetries(1);
        caseRepository.save(case4);

        // when
        var result = caseRepository.findByIsRetentionUpdatedTrueAndRetentionRetriesLessThan(3);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(case1.getId());
    }
}
