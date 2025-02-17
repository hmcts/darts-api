package uk.gov.hmcts.darts.arm.rpo;

import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.StorageAccountResponse;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ArmRpoStateEnum;
import uk.gov.hmcts.darts.common.enums.ArmRpoStatusEnum;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


class GetStorageAccountsServiceIntTest extends IntegrationBase {

    @MockitoBean
    private ArmRpoClient armRpoClient;

    @MockitoBean
    private ArmApiConfigurationProperties armApiConfigurationProperties;

    @Autowired
    private GetStorageAccountsService getStorageAccountsService;


    @Test
    void getStorageAccountsSuccess() {
        // given
        when(armApiConfigurationProperties.getArmStorageAccountName()).thenReturn("expectedAccountName");

        StorageAccountResponse.DataDetails dataDetails1 = new StorageAccountResponse.DataDetails();
        dataDetails1.setId("indexId1");
        dataDetails1.setName("unexpectedAccountName");

        StorageAccountResponse.DataDetails dataDetails2 = new StorageAccountResponse.DataDetails();
        dataDetails2.setId("indexId2");
        dataDetails2.setName("expectedAccountName");

        StorageAccountResponse storageAccountResponse = new StorageAccountResponse();
        storageAccountResponse.setStatus(200);
        storageAccountResponse.setIsError(false);
        storageAccountResponse.setDataDetails(List.of(dataDetails1, dataDetails2));

        var bearerAuth = "Bearer some-token";
        when(armRpoClient.getStorageAccounts(any(), any())).thenReturn(storageAccountResponse);

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setCreatedBy(userAccount);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccount);
        var armRpoExecutionDetail = dartsPersistence.save(armRpoExecutionDetailEntity);

        // when
        getStorageAccountsService.getStorageAccounts(bearerAuth, armRpoExecutionDetail.getId(), userAccount);

        // then
        var armRpoExecutionDetailEntityUpdated = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetail.getId()).get();
        assertEquals(ArmRpoStateEnum.GET_STORAGE_ACCOUNTS.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.COMPLETED.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoStatus().getId());
        assertEquals("indexId2", armRpoExecutionDetailEntityUpdated.getStorageAccountId());
    }

    @Test
    void getStorageAccountsWithMissingMatchingStorage() {

        // given
        StorageAccountResponse response = new StorageAccountResponse();
        response.setStatus(200);
        response.setIsError(false);
        when(armRpoClient.getStorageAccounts(any(), any())).thenReturn(response);

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setCreatedBy(userAccount);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccount);
        var armRpoExecutionDetail = dartsPersistence.save(armRpoExecutionDetailEntity);

        var bearerAuth = "Bearer some-token";

        // when
        ArmRpoException armRpoException = assertThrows(
            ArmRpoException.class, () -> getStorageAccountsService.getStorageAccounts(bearerAuth, armRpoExecutionDetail.getId(), userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM get storage accounts: No data details were present in the storage account response"));

        var armRpoExecutionDetailEntityUpdated = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetail.getId()).get();
        assertEquals(ArmRpoStateEnum.GET_STORAGE_ACCOUNTS.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.FAILED.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoStatus().getId());
        assertNull(armRpoExecutionDetailEntityUpdated.getStorageAccountId());
    }

    @Test
    void getStorageAccountsFailsWhenClientThrowsFeignException() {

        // given
        when(armRpoClient.getStorageAccounts(any(), any())).thenThrow(FeignException.BadRequest.class);

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setCreatedBy(userAccount);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccount);
        var armRpoExecutionDetail = dartsPersistence.save(armRpoExecutionDetailEntity);

        var bearerAuth = "Bearer some-token";

        // when
        ArmRpoException armRpoException = assertThrows(
            ArmRpoException.class, () -> getStorageAccountsService.getStorageAccounts(bearerAuth, armRpoExecutionDetail.getId(), userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM get storage accounts: Unable to get ARM RPO response"));
        var armRpoExecutionDetailEntityUpdated = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetail.getId()).get();
        assertEquals(ArmRpoStateEnum.GET_STORAGE_ACCOUNTS.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.FAILED.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoStatus().getId());
        assertNull(armRpoExecutionDetailEntityUpdated.getStorageAccountId());
    }


}
