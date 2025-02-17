package uk.gov.hmcts.darts.arm.rpo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.EmptyRpoRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.ProfileEntitlementResponse;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ArmRpoStateEnum;
import uk.gov.hmcts.darts.common.enums.ArmRpoStatusEnum;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@TestPropertySource(properties = {
    "darts.storage.arm-api.arm-service-entitlement=SRV-DARTS-RW-E"
})
class GetProfileEntitlementsServiceIntTest extends PostgresIntegrationBase {

    @MockitoBean
    private ArmRpoClient armRpoClient;

    @Autowired
    private GetProfileEntitlementsService getProfileEntitlementsService;

    private static final String TOKEN = "some token";
    private static final String ENTITLEMENT_ID = "some entitlement id";
    private static final String ENTITLEMENT_NAME = "SRV-DARTS-RW-E";

    @Test
    void getProfileEntitlements_shouldSucceed_whenASuccessResponseIsObtainedFromArmThatContainsAMatchingEntitlement() {
        // Given
        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();

        var executionDetailEntity = createExecutionDetailEntity(userAccount);
        executionDetailEntity = dartsPersistence.save(executionDetailEntity);

        Integer executionId = executionDetailEntity.getId();

        var profileEntitlement = new ProfileEntitlementResponse.ProfileEntitlement();
        profileEntitlement.setName(ENTITLEMENT_NAME);
        profileEntitlement.setEntitlementId(ENTITLEMENT_ID);
        createEntitlementResponseAndSetMock(Collections.singletonList(profileEntitlement));

        // When
        getProfileEntitlementsService.getProfileEntitlements(TOKEN, executionId, userAccount);

        // Then
        executionDetailEntity = dartsPersistence.getArmRpoExecutionDetailRepository().findById(executionId)
            .orElseThrow();
        assertEquals(ArmRpoStateEnum.GET_PROFILE_ENTITLEMENTS.getId(), executionDetailEntity.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.COMPLETED.getId(), executionDetailEntity.getArmRpoStatus().getId());
        assertEquals(ENTITLEMENT_ID, executionDetailEntity.getEntitlementId());
    }

    @Test
    void getProfileEntitlements_shouldFail_whenArmResponseDoesNotContainAMatchingEntitlement() {
        // Given
        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();

        var executionDetailEntity = createExecutionDetailEntity(userAccount);
        executionDetailEntity = dartsPersistence.save(executionDetailEntity);

        Integer executionId = executionDetailEntity.getId();

        var profileEntitlement = new ProfileEntitlementResponse.ProfileEntitlement();
        profileEntitlement.setName("some other entitlement");
        createEntitlementResponseAndSetMock(Collections.singletonList(profileEntitlement));

        // When
        String exceptionMessage = assertThrows(ArmRpoException.class, () ->
            getProfileEntitlementsService.getProfileEntitlements(TOKEN, executionId, userAccount))
            .getMessage();

        // Then
        assertThat(exceptionMessage, containsString("ARM getProfileEntitlements: No matching entitlements 'SRV-DARTS-RW-E' were returned"));

        executionDetailEntity = dartsPersistence.getArmRpoExecutionDetailRepository().findById(executionId)
            .orElseThrow();
        assertEquals(ArmRpoStateEnum.GET_PROFILE_ENTITLEMENTS.getId(), executionDetailEntity.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.FAILED.getId(), executionDetailEntity.getArmRpoStatus().getId());
        assertNull(executionDetailEntity.getEntitlementId());
    }

    private ArmRpoExecutionDetailEntity createExecutionDetailEntity(UserAccountEntity userAccount) {
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setCreatedBy(userAccount);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccount);
        return armRpoExecutionDetailEntity;
    }

    private void createEntitlementResponseAndSetMock(List<ProfileEntitlementResponse.ProfileEntitlement> profileEntitlements) {
        var response = new ProfileEntitlementResponse();
        response.setStatus(200);
        response.setIsError(false);
        response.setEntitlements(profileEntitlements);
        EmptyRpoRequest emptyRpoRequest = EmptyRpoRequest.builder().build();
        when(armRpoClient.getProfileEntitlementResponse(TOKEN, emptyRpoRequest))
            .thenReturn(response);
    }

}
