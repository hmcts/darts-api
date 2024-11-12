package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import static org.assertj.core.api.Assertions.assertThat;

class ArmRpoExecutionDetailRepositoryTest extends PostgresIntegrationBase {

    @Autowired
    private ArmRpoExecutionDetailRepository armRpoExecutionDetailRepository;

    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity1;
    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity2;
    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity3;


    @BeforeEach
    public void beforeAll() {
        armRpoExecutionDetailEntity1 = PersistableFactory.getArmRpoExecutionDetailTestData().minimalArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity2 = PersistableFactory.getArmRpoExecutionDetailTestData().minimalArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity3 = PersistableFactory.getArmRpoExecutionDetailTestData().minimalArmRpoExecutionDetailEntity();
    }

    @Test
    void findByCreatedDateTimeDesc_shouldReturnExpectedArmRpoExecutionDetail_whenItExists() {
        // when
        var result = armRpoExecutionDetailRepository.findByCreatedDateTimeDesc();

        // then
        assertThat(result).isEqualTo(armRpoExecutionDetailEntity3);
    }

    /*@Test
    void findByCreatedDateTimeDesc_shouldReturnEmptyList_whenNoArmRpoExecutionDetailExists() {
        // Given we have no arm rpo execution details
        // When we try to find them
        var armRpoExecutionDetails = armRpoExecutionDetailRepository.findByCreatedDateTimeDesc();
        // Then we get an empty list
        assertThat(armRpoExecutionDetails, empty());
    }*/
}
