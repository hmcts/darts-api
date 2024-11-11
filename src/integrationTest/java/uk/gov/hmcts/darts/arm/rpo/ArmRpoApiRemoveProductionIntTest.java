package uk.gov.hmcts.darts.arm.rpo;

import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.RemoveProductionResponse;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ArmRpoStateEnum;
import uk.gov.hmcts.darts.common.enums.ArmRpoStatusEnum;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ArmRpoApiRemoveProductionIntTest extends IntegrationBase {

    @MockBean
    private ArmRpoClient armRpoClient;

    @Autowired
    private ArmRpoApi armRpoApi;


    @Test
    void removeProductionSuccess() {

        // given
        RemoveProductionResponse response = new RemoveProductionResponse();
        response.setStatus(200);
        response.setIsError(false);
        when(armRpoClient.removeProduction(any(), any())).thenReturn(response);

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setCreatedBy(userAccount);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccount);
        armRpoExecutionDetailEntity.setProductionId("productionId");
        var armRpoExecutionDetail = dartsPersistence.save(armRpoExecutionDetailEntity);

        var bearerAuth = "Bearer some-token";

        // when
        armRpoApi.removeProduction(bearerAuth, armRpoExecutionDetail.getId(), userAccount);

        // then
        var armRpoExecutionDetailEntityUpdated = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetail.getId()).orElseThrow();
        assertEquals(ArmRpoStateEnum.REMOVE_PRODUCTION.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.COMPLETED.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoStatus().getId());

    }

    @Test
    void removeProductionThrowsFeignException() {

        // given
        when(armRpoClient.removeProduction(any(), any())).thenThrow(FeignException.BadRequest.class);

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setCreatedBy(userAccount);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccount);
        armRpoExecutionDetailEntity.setProductionId("productionId");
        var armRpoExecutionDetail = dartsPersistence.save(armRpoExecutionDetailEntity);

        var bearerAuth = "Bearer some-token";

        // when
        armRpoApi.removeProduction(bearerAuth, armRpoExecutionDetail.getId(), userAccount);

        // then
        var armRpoExecutionDetailEntityUpdated = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetail.getId()).orElseThrow();
        assertEquals(ArmRpoStateEnum.REMOVE_PRODUCTION.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.FAILED.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoStatus().getId());

    }
}
