package uk.gov.hmcts.darts.arm.rpo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.SaveBackgroundSearchResponse;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ArmRpoStateEnum;
import uk.gov.hmcts.darts.common.enums.ArmRpoStatusEnum;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ArmRpoApiSaveBackgroundSearchIntTest extends IntegrationBase {

    @MockBean
    private ArmRpoClient armRpoClient;

    @Autowired
    private ArmRpoApi armRpoApi;


    @Test
    void saveBackgroundSearchSuccess() {

        // given
        SaveBackgroundSearchResponse response = new SaveBackgroundSearchResponse();
        response.setStatus(200);
        response.setIsError(false);
        when(armRpoClient.saveBackgroundSearch(any(), any())).thenReturn(response);

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setCreatedBy(userAccount);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccount);
        var armRpoExecutionDetail = dartsPersistence.save(armRpoExecutionDetailEntity);

        var bearerAuth = "Bearer some-token";

        // when
        armRpoApi.saveBackgroundSearch(bearerAuth, armRpoExecutionDetail.getId(), "searchName", userAccount);

        // then
        var armRpoExecutionDetailEntityUpdated = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetail.getId()).orElseThrow();
        assertEquals(ArmRpoStateEnum.SAVE_BACKGROUND_SEARCH.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.COMPLETED.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoStatus().getId());

    }

    @Test
    void saveBackgroundSearchReturnsResponseWithInvalidStatus() {

        // given
        SaveBackgroundSearchResponse response = new SaveBackgroundSearchResponse();
        response.setStatus(400);
        response.setIsError(true);
        when(armRpoClient.saveBackgroundSearch(any(), any())).thenReturn(response);

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setCreatedBy(userAccount);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccount);
        var armRpoExecutionDetail = dartsPersistence.save(armRpoExecutionDetailEntity);

        var bearerAuth = "Bearer some-token";

        // when
        ArmRpoException armRpoException = assertThrows(
            ArmRpoException.class, () -> armRpoApi.saveBackgroundSearch(bearerAuth, armRpoExecutionDetail.getId(), "searchName", userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM save background search: ARM RPO API failed with status - 400 BAD_REQUEST and response"));

        var armRpoExecutionDetailEntityUpdated = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetail.getId()).orElseThrow();
        assertEquals(ArmRpoStateEnum.SAVE_BACKGROUND_SEARCH.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.FAILED.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoStatus().getId());

    }

    @Test
    void saveBackgroundSearchReturnsResponseWithNullStatus() {

        // given
        SaveBackgroundSearchResponse response = new SaveBackgroundSearchResponse();
        response.setIsError(false);
        when(armRpoClient.saveBackgroundSearch(any(), any())).thenReturn(response);

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setCreatedBy(userAccount);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccount);
        var armRpoExecutionDetail = dartsPersistence.save(armRpoExecutionDetailEntity);

        var bearerAuth = "Bearer some-token";

        // when
        ArmRpoException armRpoException = assertThrows(
            ArmRpoException.class, () -> armRpoApi.saveBackgroundSearch(bearerAuth, armRpoExecutionDetail.getId(), "searchName", userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM save background search: ARM RPO API response is invalid"));

        var armRpoExecutionDetailEntityUpdated = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetail.getId()).orElseThrow();
        assertEquals(ArmRpoStateEnum.SAVE_BACKGROUND_SEARCH.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.FAILED.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoStatus().getId());

    }
}
