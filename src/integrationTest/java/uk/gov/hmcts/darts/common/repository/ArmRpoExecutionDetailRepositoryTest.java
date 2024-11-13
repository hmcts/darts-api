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
        armRpoExecutionDetailEntity1 = dartsPersistence.save(PersistableFactory.getArmRpoExecutionDetailTestData().minimalArmRpoExecutionDetailEntity());
        armRpoExecutionDetailEntity2 = dartsPersistence.save(PersistableFactory.getArmRpoExecutionDetailTestData().minimalArmRpoExecutionDetailEntity());
        armRpoExecutionDetailEntity3 = dartsPersistence.save(PersistableFactory.getArmRpoExecutionDetailTestData().minimalArmRpoExecutionDetailEntity());
    }

    @Test
    void findTopOrderByCreatedDateTimeDescShouldReturnLatestArmRpoExecutionDetail() {
        // when
        var result = armRpoExecutionDetailRepository.findTopOrderByCreatedDateTimeDesc();

        // then
        assertThat(result).isEqualTo(armRpoExecutionDetailEntity3);
    }

}
