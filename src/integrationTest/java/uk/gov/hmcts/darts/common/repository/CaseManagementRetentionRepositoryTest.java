package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
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

    @Autowired
    private EntityGraphPersistence entityGraphPersistence;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Test
    void getCaseManagementRetentionIdsForEvents() {
        var caseManagementRetention = entityGraphPersistence.persist(someMinimalCaseManagementRetention());

        var cmrIds = caseManagementRetentionRepository.getIdsForEvents(List.of(caseManagementRetention.getEventEntity()));

        assertThat(cmrIds).hasSize(1);
        assertThat(cmrIds.get(0)).isEqualTo(caseManagementRetention.getId());
    }

    @Test
    void deletesCaseManagementRetentionsForAssociatedWithEvents() {
        var caseManagementRetentionsWithEvents = createSomeCmrWithEvents(3);

        entityGraphPersistence.persistAll(caseManagementRetentionsWithEvents);

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
            .mapToObj(i -> {
                CaseManagementRetentionEntity caseManagementRetentionEntity = someMinimalCaseManagementRetention();
                // There appears to be a bug in EntityGraphPersistence that stops child UserAccounts being saved. So this is a kludge,
                // we set the user account fields to some already existing value.
                UserAccountEntity someUser = userAccountRepository.getReferenceById(0);
                CourtCaseEntity courtCase = caseManagementRetentionEntity.getCourtCase();
                courtCase.setCreatedBy(someUser);
                courtCase.setLastModifiedBy(someUser);
                return caseManagementRetentionEntity;
            })
            .collect(toList());
    }
}
