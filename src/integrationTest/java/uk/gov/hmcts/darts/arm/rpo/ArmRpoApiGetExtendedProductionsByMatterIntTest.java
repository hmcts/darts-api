package uk.gov.hmcts.darts.arm.rpo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.ExtendedProductionsByMatterResponse;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ArmRpoStateEnum;
import uk.gov.hmcts.darts.common.enums.ArmRpoStatusEnum;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.arm.enums.ArmRpoResponseStatusCode.READY_STATUS;

class ArmRpoApiGetExtendedProductionsByMatterIntTest extends IntegrationBase {

    private static final String PRODUCTION_NAME = "DARTS_RPO_2024-08-13";
    public static final String END_PRODUCTION_TIME = "2025-01-16T12:30:09.9129726+00:00";
    @MockBean
    private ArmRpoClient armRpoClient;

    @MockBean
    private ArmApiConfigurationProperties armApiConfigurationProperties;

    @Autowired
    private ArmRpoApi armRpoApi;

    @Test
    void getExtendedSearchesByMatter_ReturnsTrue() {
        // given
        ExtendedProductionsByMatterResponse extendedProductionsByMatterResponse = new ExtendedProductionsByMatterResponse();
        extendedProductionsByMatterResponse.setStatus(200);
        extendedProductionsByMatterResponse.setIsError(false);
        ExtendedProductionsByMatterResponse.Productions productions = new ExtendedProductionsByMatterResponse.Productions();
        productions.setProductionId("1234");
        productions.setStatus(READY_STATUS.getStatusCode());
        productions.setName(PRODUCTION_NAME);
        productions.setEndProductionTime(END_PRODUCTION_TIME);
        extendedProductionsByMatterResponse.setProductions(List.of(productions));

        when(armRpoClient.getExtendedProductionsByMatter(any(), any())).thenReturn(extendedProductionsByMatterResponse);

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setMatterId("1");
        armRpoExecutionDetailEntity.setCreatedBy(userAccount);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccount);
        var armRpoExecutionDetail = dartsPersistence.save(armRpoExecutionDetailEntity);
        var bearerAuth = "Bearer some-token";

        // when
        var result = armRpoApi.getExtendedProductionsByMatter(bearerAuth, armRpoExecutionDetail.getId(), PRODUCTION_NAME, userAccount);

        // then
        assertTrue(result);
        var armRpoExecutionDetailEntityUpdated = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetail.getId()).get();
        assertEquals(ArmRpoStateEnum.GET_EXTENDED_PRODUCTIONS_BY_MATTER.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.COMPLETED.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoStatus().getId());
        assertEquals("1234", armRpoExecutionDetailEntityUpdated.getProductionId());

    }

    @Test
    void getExtendedSearchesByMatter_ReturnsFalse_WhenEndProductionTimeIsNullInProgress() {
        // given
        ExtendedProductionsByMatterResponse extendedProductionsByMatterResponse = new ExtendedProductionsByMatterResponse();
        extendedProductionsByMatterResponse.setStatus(200);
        extendedProductionsByMatterResponse.setIsError(false);
        ExtendedProductionsByMatterResponse.Productions productions = new ExtendedProductionsByMatterResponse.Productions();
        productions.setProductionId("1234");
        productions.setStatus(2);
        productions.setName(PRODUCTION_NAME);
        extendedProductionsByMatterResponse.setProductions(List.of(productions));

        when(armRpoClient.getExtendedProductionsByMatter(any(), any())).thenReturn(extendedProductionsByMatterResponse);

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setMatterId("1");
        armRpoExecutionDetailEntity.setCreatedBy(userAccount);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccount);
        var armRpoExecutionDetail = dartsPersistence.save(armRpoExecutionDetailEntity);
        var bearerAuth = "Bearer some-token";

        // when
        var result = armRpoApi.getExtendedProductionsByMatter(bearerAuth, armRpoExecutionDetail.getId(), PRODUCTION_NAME, userAccount);

        // then
        assertFalse(result);
        var armRpoExecutionDetailEntityUpdated = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetail.getId()).get();
        assertEquals(ArmRpoStateEnum.GET_EXTENDED_PRODUCTIONS_BY_MATTER.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.IN_PROGRESS.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoStatus().getId());

    }
}
