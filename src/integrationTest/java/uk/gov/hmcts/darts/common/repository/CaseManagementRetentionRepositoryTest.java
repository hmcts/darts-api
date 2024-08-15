package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.EntityGraphPersistence;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.test.common.data.CaseManagementRetentionTestData.someMinimalCaseManagementRetention;

class CaseManagementRetentionRepositoryTest extends PostgresIntegrationBase {

    @Autowired
    private CaseManagementRetentionRepository caseManagementRetentionRepository;

    @Test
    void getCaseManagementRetentionIdsForEvents() {
        var caseManagementRetention = dartsDatabase.save(someMinimalCaseManagementRetention());

        var cmrIds = caseManagementRetentionRepository.getIdsForEvents(List.of(caseManagementRetention.getEventEntity()));

        assertThat(cmrIds).hasSize(1);
        assertThat(cmrIds.get(0)).isEqualTo(caseManagementRetention.getId());
    }

    @Test
    void deletesCaseManagementRetentionsForAssociatedWithEvents() {
        var caseManagementRetentionsWithEvents = createSomeCmrWithEvents(3);
        caseManagementRetentionsWithEvents.forEach(cmre -> dartsDatabase.save(cmre));

        caseManagementRetentionRepository.deleteAllByEventEntityIn(
            asList(
                caseManagementRetentionsWithEvents.get(0).getEventEntity(),
                caseManagementRetentionsWithEvents.get(1).getEventEntity()));

        assertThat(dartsDatabase.getCaseManagementRetentionRepository().findAll())
            .extracting("id")
            .containsExactly(caseManagementRetentionsWithEvents.get(2).getId());
    }

    private List<CaseManagementRetentionEntity> createSomeCmrWithEvents(int quantity) {
        return range(0, quantity)
            .mapToObj(i -> someMinimalCaseManagementRetention())
            .collect(toList());
    }
}
