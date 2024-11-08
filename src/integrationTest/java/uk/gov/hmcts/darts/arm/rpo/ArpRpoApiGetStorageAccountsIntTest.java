package uk.gov.hmcts.darts.arm.rpo;

import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
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


class ArpRpoApiGetStorageAccountsIntTest extends IntegrationBase {

    @MockBean
    private ArmRpoClient armRpoClient;

    @MockBean
    private ArmApiConfigurationProperties armApiConfigurationProperties;

    @Autowired
    private ArmRpoApi armRpoApi;


    @Test
    void getStorageShouldSucceedIfServerReturns200Success() {

        // given
        when(armApiConfigurationProperties.getArmStorageAccountName()).thenReturn("expectedAccountName");

        StorageAccountResponse.IndexDetails indexDetails1 = new StorageAccountResponse.IndexDetails();
        indexDetails1.setIndexId("indexId1");
        indexDetails1.setName("unexpectedAccountName");
        StorageAccountResponse.IndexDetails indexDetails2 = new StorageAccountResponse.IndexDetails();
        indexDetails2.setIndexId("indexId2");
        indexDetails2.setName("expectedAccountName");
        StorageAccountResponse.Index index1 = new StorageAccountResponse.Index();
        index1.setIndex(indexDetails1);
        StorageAccountResponse.Index index2 = new StorageAccountResponse.Index();
        index2.setIndex(indexDetails2);

        StorageAccountResponse storageAccountResponse = new StorageAccountResponse();
        storageAccountResponse.setIndexes(List.of(index1, index2));

        var bearerAuth = "Bearer some-token";
        when(armRpoClient.getStorageAccounts(any(), any())).thenReturn(storageAccountResponse);

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setCreatedBy(userAccount);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccount);
        var armRpoExecutionDetail = dartsPersistence.save(armRpoExecutionDetailEntity);


        // when
        armRpoApi.getStorageAccounts(bearerAuth, armRpoExecutionDetail.getId(), userAccount);

        // then
        var armRpoExecutionDetailEntityUpdated = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetail.getId()).get();
        assertEquals(ArmRpoStateEnum.GET_STORAGE_ACCOUNTS.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.COMPLETED.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoStatus().getId());
        assertEquals("indexId2", armRpoExecutionDetailEntityUpdated.getStorageAccountId());

    }

    @Test
    void getStorageShouldFailIfServerReturns200SuccessWithMissingMatchingStorage() {

        // given
        StorageAccountResponse response = new StorageAccountResponse();
        when(armRpoClient.getStorageAccounts(any(), any())).thenReturn(response);

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setCreatedBy(userAccount);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccount);
        var armRpoExecutionDetail = dartsPersistence.save(armRpoExecutionDetailEntity);

        var bearerAuth = "Bearer some-token";

        // when
        ArmRpoException armRpoException = assertThrows(
            ArmRpoException.class, () -> armRpoApi.getStorageAccounts(bearerAuth, armRpoExecutionDetail.getId(), userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM get storage accounts: Unable to get indexes from storage account response"));

        var armRpoExecutionDetailEntityUpdated = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetail.getId()).get();
        assertEquals(ArmRpoStateEnum.GET_STORAGE_ACCOUNTS.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.FAILED.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoStatus().getId());
        assertNull(armRpoExecutionDetailEntityUpdated.getStorageAccountId());
    }

    @Test
    void getStorageAccountsFailsWhenClientReturns400Error() {

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
            ArmRpoException.class, () -> armRpoApi.getStorageAccounts(bearerAuth, armRpoExecutionDetail.getId(), userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM get storage accounts: Unable to get ARM RPO response"));
        var armRpoExecutionDetailEntityUpdated = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetail.getId()).get();
        assertEquals(ArmRpoStateEnum.GET_STORAGE_ACCOUNTS.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.FAILED.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoStatus().getId());
        assertNull(armRpoExecutionDetailEntityUpdated.getStorageAccountId());
    }


}
