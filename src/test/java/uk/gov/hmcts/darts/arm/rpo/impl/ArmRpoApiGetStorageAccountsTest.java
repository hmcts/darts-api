package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.ArgumentCaptor;
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

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

    private ArgumentCaptor<ArmRpoExecutionDetailEntity> executionDetailCaptor;

    private UserAccountEntity userAccount;
    private static final Integer EXECUTION_ID = 1;
    private static final ArmRpoHelperMocks ARM_RPO_HELPER_MOCKS = new ArmRpoHelperMocks();


    @BeforeEach
    void setUp() {
        userAccount = new UserAccountEntity();

        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(EXECUTION_ID);
        when(armRpoService.getArmRpoExecutionDetailEntity(EXECUTION_ID)).thenReturn(armRpoExecutionDetailEntity);

        executionDetailCaptor = ArgumentCaptor.forClass(ArmRpoExecutionDetailEntity.class);
    }

    @Test
    void getStorageAccounts_shouldReturnsSuccess_whenSingularMatchingNamesExist() {
        // given
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
        when(armRpoClient.getStorageAccounts(anyString(), any(StorageAccountRequest.class))).thenReturn(storageAccountResponse);
        when(armApiConfigurationProperties.getArmStorageAccountName()).thenReturn("expectedAccountName");

        // when
        armRpoApi.getStorageAccounts("token", 1, userAccount);

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetStorageAccountsRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(executionDetailCaptor.capture(),
                                                 eq(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus()),
                                                 any());

        assertEquals("indexId2", executionDetailCaptor.getValue().getStorageAccountId());
    }

    @Test
    void getStorageAccounts_shouldReturnsSuccess_andSelectFirstMatchingName_whenMultipleMatchingNamesExist() {
        // Given
        StorageAccountResponse.DataDetails dataDetails1 = new StorageAccountResponse.DataDetails();
        dataDetails1.setId("indexId1");
        dataDetails1.setName("unexpectedAccountName");

        StorageAccountResponse.DataDetails dataDetails2 = new StorageAccountResponse.DataDetails();
        dataDetails2.setId("indexId2");
        dataDetails2.setName("expectedAccountName");

        StorageAccountResponse.DataDetails dataDetails3 = new StorageAccountResponse.DataDetails();
        dataDetails3.setId("indexId3");
        dataDetails3.setName("expectedAccountName"); // Duplicate name

        StorageAccountResponse storageAccountResponse = new StorageAccountResponse();
        storageAccountResponse.setStatus(200);
        storageAccountResponse.setIsError(false);
        storageAccountResponse.setDataDetails(List.of(dataDetails1, dataDetails2, dataDetails3));
        when(armRpoClient.getStorageAccounts(anyString(), any(StorageAccountRequest.class))).thenReturn(storageAccountResponse);
        when(armApiConfigurationProperties.getArmStorageAccountName()).thenReturn("expectedAccountName");

        // When
        armRpoApi.getStorageAccounts("token", 1, userAccount);

        // Then
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetStorageAccountsRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(executionDetailCaptor.capture(),
                                                 eq(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus()),
                                                 any());

        assertEquals("indexId2", executionDetailCaptor.getValue().getStorageAccountId());
    }

    @Test
    void getStorageAccountsReturnsNonMatchingStorageName() {
        // given
        StorageAccountResponse.DataDetails dataDetails1 = new StorageAccountResponse.DataDetails();
        dataDetails1.setId("indexId1");
        dataDetails1.setName("unexpectedAccountName");

        StorageAccountResponse storageAccountResponse = new StorageAccountResponse();
        storageAccountResponse.setStatus(200);
        storageAccountResponse.setIsError(false);
        storageAccountResponse.setDataDetails(Collections.singletonList(dataDetails1));

        when(armRpoClient.getStorageAccounts(anyString(), any(StorageAccountRequest.class))).thenReturn(storageAccountResponse);
        when(armApiConfigurationProperties.getArmStorageAccountName()).thenReturn("expectedAccountName");

        // when
        ArmRpoException exception = assertThrows(ArmRpoException.class, () -> armRpoApi.getStorageAccounts("token", 1, userAccount));

        // then
        assertThat(exception.getMessage(), containsString(
            "Unable to find ARM RPO storage account in response"));
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetStorageAccountsRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    void getStorageAccounts_shouldThrowException_whenArmReturnsNullOrEmptyDataDetails(List<StorageAccountResponse.DataDetails> dataDetails) {
        // Given
        StorageAccountResponse storageAccountResponse = new StorageAccountResponse();
        storageAccountResponse.setStatus(200);
        storageAccountResponse.setIsError(false);
        storageAccountResponse.setDataDetails(dataDetails);

        when(armRpoClient.getStorageAccounts(anyString(), any(StorageAccountRequest.class))).thenReturn(storageAccountResponse);

        // When
        ArmRpoException exception = assertThrows(ArmRpoException.class, () ->
            armRpoApi.getStorageAccounts("token", EXECUTION_ID, userAccount));

        // Then
        assertThat(exception.getMessage(), containsString(
            "No data details were present in the storage account response"));
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetStorageAccountsRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    void getStorageAccounts_shouldThrowException_whenArmReturnsNullOrEmptyId(String id) {
        // Given
        StorageAccountResponse.DataDetails dataDetails = new StorageAccountResponse.DataDetails();
        dataDetails.setName("expectedAccountName");
        dataDetails.setId(id);

        StorageAccountResponse storageAccountResponse = new StorageAccountResponse();
        storageAccountResponse.setStatus(200);
        storageAccountResponse.setIsError(false);
        storageAccountResponse.setDataDetails(Collections.singletonList(dataDetails));

        when(armRpoClient.getStorageAccounts(anyString(), any(StorageAccountRequest.class))).thenReturn(storageAccountResponse);
        when(armApiConfigurationProperties.getArmStorageAccountName()).thenReturn("expectedAccountName");

        // When
        ArmRpoException exception = assertThrows(ArmRpoException.class, () ->
            armRpoApi.getStorageAccounts("token", EXECUTION_ID, userAccount));

        // Then
        assertThat(exception.getMessage(), containsString(
            "Unable to find ARM RPO storage account in response"));
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetStorageAccountsRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
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