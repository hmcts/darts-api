package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import org.junit.jupiter.api.AfterAll;
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
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelperMocks;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArmRpoApiGetStorageAccountsTest {

    @Mock
    private ArmRpoClient armRpoClient;

    @Mock
    private ArmRpoService armRpoService;

    @Mock
    private ArmRpoHelper armRpoHelper;

    @Mock
    private ArmApiConfigurationProperties armApiConfigurationProperties;

    @InjectMocks
    private ArmRpoApiImpl armRpoApi;

    private UserAccountEntity userAccount;
    private static final Integer EXECUTION_ID = 1;
    private static final ArmRpoHelperMocks ARM_RPO_HELPER_MOCKS = new ArmRpoHelperMocks();


    @BeforeEach
    void setUp() {
        userAccount = new UserAccountEntity();

        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(EXECUTION_ID);
        when(armRpoService.getArmRpoExecutionDetailEntity(EXECUTION_ID)).thenReturn(armRpoExecutionDetailEntity);
    }

    @Test
    void getStorageAccountsReturnsSuccess() {
        // given
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
        when(armRpoClient.getStorageAccounts(anyString(), any(StorageAccountRequest.class))).thenReturn(storageAccountResponse);
        when(armApiConfigurationProperties.getArmStorageAccountName()).thenReturn("expectedAccountName");

        // when
        armRpoApi.getStorageAccounts("token", 1, userAccount);

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetStorageAccountsRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus()), any());
    }

    @Test
    void getStorageAccountsReturnsNonMatchingStorageName() {
        // given
        StorageAccountResponse storageAccountResponse = new StorageAccountResponse();
        StorageAccountResponse.IndexDetails indexDetails1 = new StorageAccountResponse.IndexDetails();
        indexDetails1.setIndexId("indexId1");
        indexDetails1.setName("unexpectedAccountName");
        StorageAccountResponse.Index index1 = new StorageAccountResponse.Index();
        index1.setIndex(indexDetails1);

        storageAccountResponse.setIndexes(List.of(index1));
        when(armRpoClient.getStorageAccounts(anyString(), any(StorageAccountRequest.class))).thenReturn(storageAccountResponse);
        when(armApiConfigurationProperties.getArmStorageAccountName()).thenReturn("expectedAccountName");

        // when
        assertThrows(ArmRpoException.class, () -> armRpoApi.getStorageAccounts("token", 1, userAccount));

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetStorageAccountsRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
    }

    @Test
    void getStorageAccountsThrowsFeignException() {
        // given
        when(armRpoClient.getStorageAccounts(anyString(), any(StorageAccountRequest.class))).thenThrow(FeignException.class);

        // when
        assertThrows(ArmRpoException.class, () -> armRpoApi.getStorageAccounts("token", 1, userAccount));

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetStorageAccountsRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
    }

    @Test
    void getStorageAccountsReturnsNullResponse() {
        // given
        when(armRpoClient.getStorageAccounts(anyString(), any(StorageAccountRequest.class))).thenReturn(null);

        // when
        assertThrows(ArmRpoException.class, () -> armRpoApi.getStorageAccounts("token", 1, userAccount));

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetStorageAccountsRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
    }

    @AfterAll
    static void close() {
        ARM_RPO_HELPER_MOCKS.close();
    }
}