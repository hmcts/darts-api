package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.StorageAccountRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.StorageAccountResponse;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArmRpoApiGetStorageAccountsTest {

    @Mock
    private ArmRpoClient armRpoClient;

    @Mock
    private ArmRpoService armRpoService;

    @Mock
    private ArmApiConfigurationProperties armApiConfigurationProperties;

    @InjectMocks
    private ArmRpoApiImpl armRpoApi;

    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity;
    private UserAccountEntity userAccount;

    @BeforeEach
    void setUp() {
        armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        userAccount = new UserAccountEntity();
    }

    @Test
    void getStorageAccountsReturnsSuccess() {
        StorageAccountResponse storageAccountResponse = new StorageAccountResponse();
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
        storageAccountResponse.setIndexes(List.of(index1, index2));
        // Set up mock responses and behavior
        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.getStorageAccounts(anyString(), any(StorageAccountRequest.class))).thenReturn(storageAccountResponse);
        when(armApiConfigurationProperties.getArmStorageAccountName()).thenReturn("expectedAccountName");

        StorageAccountResponse result = armRpoApi.getStorageAccounts("token", 1, userAccount);

        assertNotNull(result);
        verify(armRpoService).updateArmRpoStateAndStatus(any(), any(), any(), any());
        verify(armRpoService).updateArmRpoStatus(any(), any(), any());
    }

    @Test
    void getStorageAccountsReturnsNonMatchingStorageName() {
        StorageAccountResponse storageAccountResponse = new StorageAccountResponse();
        StorageAccountResponse.IndexDetails indexDetails1 = new StorageAccountResponse.IndexDetails();
        indexDetails1.setIndexId("indexId1");
        indexDetails1.setName("unexpectedAccountName");
        StorageAccountResponse.Index index1 = new StorageAccountResponse.Index();
        index1.setIndex(indexDetails1);

        storageAccountResponse.setIndexes(List.of(index1));
        // Set up mock responses and behaviuor
        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.getStorageAccounts(anyString(), any(StorageAccountRequest.class))).thenReturn(storageAccountResponse);
        when(armApiConfigurationProperties.getArmStorageAccountName()).thenReturn("expectedAccountName");

        assertThrows(ArmRpoException.class, () -> armRpoApi.getStorageAccounts("token", 1, userAccount));

        verify(armRpoService).updateArmRpoStateAndStatus(any(), any(), any(), any());
        verify(armRpoService).updateArmRpoStatus(any(), any(), any());
    }

    @Test
    void getStorageAccountsThrowsFeignException() {
        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.getStorageAccounts(anyString(), any(StorageAccountRequest.class))).thenThrow(FeignException.class);

        assertThrows(ArmRpoException.class, () -> armRpoApi.getStorageAccounts("token", 1, userAccount));
        verify(armRpoService).updateArmRpoStateAndStatus(any(), any(), any(), any());
        verify(armRpoService).updateArmRpoStatus(any(), any(), any());
    }

    @Test
    void getStorageAccountsReturnsNullResponse() {
        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.getStorageAccounts(anyString(), any(StorageAccountRequest.class))).thenReturn(null);

        assertThrows(ArmRpoException.class, () -> armRpoApi.getStorageAccounts("token", 1, userAccount));
        verify(armRpoService).updateArmRpoStateAndStatus(any(), any(), any(), any());
        verify(armRpoService).updateArmRpoStatus(any(), any(), any());
    }

}