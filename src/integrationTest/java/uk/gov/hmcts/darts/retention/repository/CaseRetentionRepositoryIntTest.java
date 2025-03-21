package uk.gov.hmcts.darts.retention.repository;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.CaseRetentionStub;
import uk.gov.hmcts.darts.testutils.stubs.CourtCaseStub;
import uk.gov.hmcts.darts.testutils.stubs.EntityGraphPersistence;

import java.time.OffsetDateTime;
import java.util.List;

import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.test.common.data.CaseManagementRetentionTestData.someMinimalCaseManagementRetention;
import static uk.gov.hmcts.darts.test.common.data.CaseRetentionTestData.createCaseRetentionFor;

class CaseRetentionRepositoryIntTest extends IntegrationBase {

    private static final OffsetDateTime DT_2025 = OffsetDateTime.of(2025, 1, 1, 1, 0, 0, 0, UTC);
    private static final OffsetDateTime DT_2026 = OffsetDateTime.of(2026, 1, 1, 1, 0, 0, 0, UTC);

    @Autowired
    CourtCaseStub caseStub;
    @Autowired
    CaseRetentionStub caseRetentionStub;
    @Autowired
    CaseRetentionRepository caseRetentionRepository;
    @Autowired
    EntityGraphPersistence entityGraphPersistence;

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

    @Test
    void deleteCaseRetentionsByCaseManagementId() {
        var caseRetentionsWithCmr = someCaseRetentionsWithCmr(3);

        caseRetentionRepository.deleteAllByCaseManagementIdsIn(
            firstTwoCmrIdsFrom(caseRetentionsWithCmr));

        assertThat(dartsDatabase.getCaseRetentionRepository().findAll())
            .extracting("id")
            .containsExactly(caseRetentionsWithCmr.get(2).getCaseManagementRetention().getId());
    }

    private static @NotNull List<Integer> firstTwoCmrIdsFrom(List<CaseRetentionEntity> caseRetentionsWithCmr) {
        return asList(
            caseRetentionsWithCmr.getFirst().getCaseManagementRetention().getId(),
            caseRetentionsWithCmr.get(1).getCaseManagementRetention().getId());
    }

    private List<CaseRetentionEntity> someCaseRetentionsWithCmr(int quantity) {
        return range(0, quantity)
            .mapToObj(i -> createCaseRetentionFor(someMinimalCaseManagementRetention()))
            .peek(caseRetentionEntity -> dartsDatabase.save(caseRetentionEntity))
            .collect(toList());
    }
}