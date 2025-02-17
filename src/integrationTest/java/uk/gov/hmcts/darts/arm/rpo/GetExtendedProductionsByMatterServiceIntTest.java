package uk.gov.hmcts.darts.arm.rpo;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.ExtendedProductionsByMatterResponse;
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

class GetExtendedProductionsByMatterServiceIntTest extends IntegrationBase {

    private static final String PRODUCTION_NAME = "DARTS_RPO_2024-08-13";
    public static final String END_PRODUCTION_TIME = "2025-01-16T12:30:09.9129726+00:00";

    @MockitoBean
    private ArmRpoClient armRpoClient;

    @Autowired
    private GetExtendedProductionsByMatterService getExtendedProductionsByMatterService;

    private UserAccountEntity userAccount;
    private ArmRpoExecutionDetailEntity armRpoExecutionDetail;

    @BeforeEach
    void setup() {
        userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setMatterId("1");
        armRpoExecutionDetailEntity.setCreatedBy(userAccount);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccount);
        armRpoExecutionDetail = dartsPersistence.save(armRpoExecutionDetailEntity);
    }

    @Test
    void getExtendedProductionsByMatter_ReturnsTrue() {
        // given
        ExtendedProductionsByMatterResponse extendedProductionsByMatterResponse = getExtendedProductionsByMatterResponse();
        ExtendedProductionsByMatterResponse.Productions productions = getProductions();
        productions.setEndProductionTime(END_PRODUCTION_TIME);
        extendedProductionsByMatterResponse.setProductions(List.of(productions));

        when(armRpoClient.getExtendedProductionsByMatter(any(), any())).thenReturn(extendedProductionsByMatterResponse);

        var bearerAuth = "Bearer some-token";

        // when
        var result = getExtendedProductionsByMatterService.getExtendedProductionsByMatter(bearerAuth, armRpoExecutionDetail.getId(), PRODUCTION_NAME,
                                                                                          userAccount);

        // then
        assertTrue(result);
        var armRpoExecutionDetailEntityUpdated = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetail.getId()).get();
        assertEquals(ArmRpoStateEnum.GET_EXTENDED_PRODUCTIONS_BY_MATTER.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.COMPLETED.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoStatus().getId());
        assertEquals("1234", armRpoExecutionDetailEntityUpdated.getProductionId());

    }

    @Test
    void getExtendedProductionsByMatter_ReturnsFalseForInProgress_WhenEndProductionTimeIsNull() {
        // given
        ExtendedProductionsByMatterResponse extendedProductionsByMatterResponse = getExtendedProductionsByMatterResponse();
        ExtendedProductionsByMatterResponse.Productions productions = getProductions();
        extendedProductionsByMatterResponse.setProductions(List.of(productions));

        when(armRpoClient.getExtendedProductionsByMatter(any(), any())).thenReturn(extendedProductionsByMatterResponse);

        var bearerAuth = "Bearer some-token";

        // when
        var result = getExtendedProductionsByMatterService.getExtendedProductionsByMatter(bearerAuth, armRpoExecutionDetail.getId(), PRODUCTION_NAME,
                                                                                          userAccount);

        // then
        assertFalse(result);
        var armRpoExecutionDetailEntityUpdated = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetail.getId()).get();
        assertEquals(ArmRpoStateEnum.GET_EXTENDED_PRODUCTIONS_BY_MATTER.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.IN_PROGRESS.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoStatus().getId());

    }

    private static @NotNull ExtendedProductionsByMatterResponse getExtendedProductionsByMatterResponse() {
        ExtendedProductionsByMatterResponse extendedProductionsByMatterResponse = new ExtendedProductionsByMatterResponse();
        extendedProductionsByMatterResponse.setStatus(200);
        extendedProductionsByMatterResponse.setIsError(false);
        return extendedProductionsByMatterResponse;
    }

    private static ExtendedProductionsByMatterResponse.@NotNull Productions getProductions() {
        ExtendedProductionsByMatterResponse.Productions productions = new ExtendedProductionsByMatterResponse.Productions();
        productions.setProductionId("1234");
        productions.setName(PRODUCTION_NAME);
        return productions;
    }
}
