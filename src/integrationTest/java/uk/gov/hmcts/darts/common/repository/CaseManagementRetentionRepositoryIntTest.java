package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.test.common.data.CaseManagementRetentionTestData.someMinimalCaseManagementRetention;

class CaseManagementRetentionRepositoryIntTest extends PostgresIntegrationBase {

    @Autowired
    private CaseManagementRetentionRepository caseManagementRetentionRepository;

    @Test
    void getCaseManagementRetentionIdsForEvents() {
        var caseManagementRetention = dartsDatabase.save(someMinimalCaseManagementRetention());

        var cmrIds = caseManagementRetentionRepository.getIdsForEvents(List.of(caseManagementRetention.getEventEntity().getId()));

        assertThat(cmrIds).hasSize(1);
        assertThat(cmrIds.getFirst()).isEqualTo(caseManagementRetention.getId());
    }

    @Test
    void deletesCaseManagementRetentionsForAssociatedWithEvents() {
        var caseManagementRetentionsWithEvents = createSomeCmrWithEvents(3);

        caseManagementRetentionRepository.deleteAllByEventEntityIn(
            asList(
                caseManagementRetentionsWithEvents.getFirst().getEventEntity().getId(),
                caseManagementRetentionsWithEvents.get(1).getEventEntity().getId()));

        assertThat(dartsDatabase.getCaseManagementRetentionRepository().findAll())
            .extracting("id")
            .containsExactly(caseManagementRetentionsWithEvents.get(2).getId());
    }

    private List<CaseManagementRetentionEntity> createSomeCmrWithEvents(int quantity) {
        return range(0, quantity)
            .mapToObj(i -> someMinimalCaseManagementRetention())
            .map(caseManagementRetentionEntity -> dartsDatabase.save(caseManagementRetentionEntity))
            .collect(toList());
    }
}