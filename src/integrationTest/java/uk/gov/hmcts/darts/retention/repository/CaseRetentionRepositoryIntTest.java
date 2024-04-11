package uk.gov.hmcts.darts.retention.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.CaseRetentionStub;
import uk.gov.hmcts.darts.testutils.stubs.CourtCaseStub;

import java.time.OffsetDateTime;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

class CaseRetentionRepositoryIntTest extends IntegrationBase {

    private static final OffsetDateTime DT_2025 = OffsetDateTime.of(2025, 1, 1, 1, 0, 0, 0, UTC);
    private static final OffsetDateTime DT_2026 = OffsetDateTime.of(2026, 1, 1, 1, 0, 0, 0, UTC);

    @Autowired
    CourtCaseStub caseStub;
    @Autowired
    CaseRetentionStub caseRetentionStub;
    @Autowired
    CaseRetentionRepository caseRetentionRepository;

    @Test
    void testFindTopByCourtCaseOrderByRetainUntilAppliedOnDesc() {

        // given
        var courtCase = caseStub.createAndSaveCourtCase(createdCourtCase -> {
            createdCourtCase.setRetentionUpdated(true);
            createdCourtCase.setRetentionRetries(1);
            createdCourtCase.setClosed(true);
        });

        caseRetentionStub.createCaseRetentionObject(courtCase, DT_2025);
        caseRetentionStub.createCaseRetentionObject(courtCase, DT_2026);

        // when
        var caseAResult = caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(courtCase).get();

        // then
        assertThat(caseAResult.getRetainUntil()).isEqualTo(DT_2026);
    }
}
