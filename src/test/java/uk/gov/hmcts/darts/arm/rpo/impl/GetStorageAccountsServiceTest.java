package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.StorageAccountRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.StorageAccountResponse;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelperMocks;
import uk.gov.hmcts.darts.arm.service.ArmApiService;
import uk.gov.hmcts.darts.arm.service.ArmClientService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.service.impl.ArmClientServiceImpl;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.CloseResource"})
class GetStorageAccountsServiceTest {

    private static final Integer EXECUTION_ID = 1;
    private static final String BEARER_TOKEN = "token";

    @Mock
    private ArmRpoClient armRpoClient;
    @Mock
    private ArmApiService armApiService;
    @Mock
    private ArmRpoService armRpoService;
    @Mock
    private ArmApiConfigurationProperties armApiConfigurationProperties;

    private ArmRpoUtil armRpoUtil;

    private GetStorageAccountsServiceImpl getStorageAccountsService;

    private ArgumentCaptor<ArmRpoExecutionDetailEntity> executionDetailCaptor;

    private UserAccountEntity userAccount;
    private ArmRpoHelperMocks armRpoHelperMocks;
    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity;

    @BeforeEach
    void setUp() {
        armRpoHelperMocks = new ArmRpoHelperMocks();
        userAccount = new UserAccountEntity();

        armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(EXECUTION_ID);
        lenient().when(armRpoService.getArmRpoExecutionDetailEntity(EXECUTION_ID)).thenReturn(armRpoExecutionDetailEntity);

        executionDetailCaptor = ArgumentCaptor.forClass(ArmRpoExecutionDetailEntity.class);
        armRpoUtil = spy(new ArmRpoUtil(armRpoService, armApiService));
        ArmClientService armClientService = new ArmClientServiceImpl(null, null, armRpoClient);
        getStorageAccountsService = new GetStorageAccountsServiceImpl(armClientService, armRpoService, armRpoUtil, armApiConfigurationProperties);
    }

    @Test
    void getStorageAccounts_shouldReturnsSuccess_whenSingularMatchingNamesExist() {
        // given
        StorageAccountResponse storageAccountResponse = getStorageAccountResponse();
        when(armRpoClient.getStorageAccounts(anyString(), any(StorageAccountRequest.class))).thenReturn(storageAccountResponse);
        when(armApiConfigurationProperties.getArmStorageAccountName()).thenReturn("expectedAccountName");

        // when
        getStorageAccountsService.getStorageAccounts(BEARER_TOKEN, 1, userAccount);

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getGetStorageAccountsRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(executionDetailCaptor.capture(),
                                                 eq(armRpoHelperMocks.getCompletedRpoStatus()),
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
        getStorageAccountsService.getStorageAccounts(BEARER_TOKEN, 1, userAccount);

        // Then
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getGetStorageAccountsRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(executionDetailCaptor.capture(),
                                                 eq(armRpoHelperMocks.getCompletedRpoStatus()),
                                                 any());

        assertEquals("indexId2", executionDetailCaptor.getValue().getStorageAccountId());
    }

    @Test
    void getStorageAccounts_ReturnsNonMatchingStorageName() {
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
        ArmRpoException exception = assertThrows(ArmRpoException.class, () -> getStorageAccountsService.getStorageAccounts(BEARER_TOKEN, 1, userAccount));

        // then
        assertThat(exception.getMessage(), containsString(
            "Unable to find ARM RPO storage account in response"));
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getGetStorageAccountsRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
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
            getStorageAccountsService.getStorageAccounts(BEARER_TOKEN, EXECUTION_ID, userAccount));

        // Then
        assertThat(exception.getMessage(), containsString(
            "No data details were present in the storage account response"));
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getGetStorageAccountsRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
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
            getStorageAccountsService.getStorageAccounts(BEARER_TOKEN, EXECUTION_ID, userAccount));

        // Then
        assertThat(exception.getMessage(), containsString(
            "Unable to find ARM RPO storage account in response"));
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getGetStorageAccountsRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());

    }

    @Test
    void getStorageAccounts_ThrowsFeignException() {
        // given
        when(armRpoClient.getStorageAccounts(anyString(), any(StorageAccountRequest.class))).thenThrow(FeignException.class);

        // when
        assertThrows(ArmRpoException.class, () -> getStorageAccountsService.getStorageAccounts(BEARER_TOKEN, 1, userAccount));

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getGetStorageAccountsRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
    }

    @Test
    void getStorageAccounts_ReturnsNullResponse() {
        // given
        when(armRpoClient.getStorageAccounts(anyString(), any(StorageAccountRequest.class))).thenReturn(null);

        // when
        assertThrows(ArmRpoException.class, () -> getStorageAccountsService.getStorageAccounts(BEARER_TOKEN, 1, userAccount));

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getGetStorageAccountsRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
    }

    @Test
    void getStorageAccounts_shouldRetryOnUnauthorised_thenSucceed() {
        // given
        Response response = Response.builder()
            .request(Request.create(Request.HttpMethod.POST, "/getStorageAccounts", java.util.Map.of(), null, StandardCharsets.UTF_8, null))
            .status(401)
            .reason("Unauthorized")
            .build();
        FeignException feign401 = FeignException.errorStatus("getStorageAccounts", response);

        // First call throws 401
        when(armRpoClient.getStorageAccounts(eq(BEARER_TOKEN), any(StorageAccountRequest.class))).thenThrow(feign401);

        // armRpoUtil should be asked for a new token
        doReturn("Bearer refreshed").when(armRpoUtil).retryGetBearerToken(anyString());

        StorageAccountResponse storageAccountResponse = getStorageAccountResponse();
        when(armRpoClient.getStorageAccounts(eq("Bearer refreshed"), any(StorageAccountRequest.class))).thenReturn(storageAccountResponse);
        when(armApiConfigurationProperties.getArmStorageAccountName()).thenReturn("expectedAccountName");

        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);

        // when
        getStorageAccountsService.getStorageAccounts(BEARER_TOKEN, 1, userAccount);

        // then
        verify(armRpoClient).getStorageAccounts(eq(BEARER_TOKEN), any(StorageAccountRequest.class));
        verify(armRpoUtil).retryGetBearerToken(anyString());
        verify(armRpoClient).getStorageAccounts(eq("Bearer refreshed"), any(StorageAccountRequest.class));
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getGetStorageAccountsRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getCompletedRpoStatus()), any());
    }

    @Test
    void getStorageAccounts_shouldRetryOnForbidden_thenSucceed() {
        // given
        Response response = Response.builder()
            .request(Request.create(Request.HttpMethod.POST, "/getStorageAccounts", java.util.Map.of(), null, StandardCharsets.UTF_8, null))
            .status(403)
            .reason("Forbidden")
            .build();
        FeignException feign403 = FeignException.errorStatus("getStorageAccounts", response);

        when(armRpoClient.getStorageAccounts(eq(BEARER_TOKEN), any(StorageAccountRequest.class)))
            .thenThrow(feign403);

        doReturn("Bearer refreshed").when(armRpoUtil).retryGetBearerToken(anyString());

        StorageAccountResponse storageAccountResponse = getStorageAccountResponse();
        when(armRpoClient.getStorageAccounts(eq("Bearer refreshed"), any(StorageAccountRequest.class))).thenReturn(storageAccountResponse);
        when(armApiConfigurationProperties.getArmStorageAccountName()).thenReturn("expectedAccountName");

        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);

        // when
        getStorageAccountsService.getStorageAccounts(BEARER_TOKEN, 1, userAccount);

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getGetStorageAccountsRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(executionDetailCaptor.capture(),
                                                 eq(armRpoHelperMocks.getCompletedRpoStatus()),
                                                 any());

        assertEquals("indexId2", executionDetailCaptor.getValue().getStorageAccountId());
        verify(armRpoClient).getStorageAccounts(eq(BEARER_TOKEN), any(StorageAccountRequest.class));
        verify(armRpoUtil).retryGetBearerToken("getStorageAccounts");
        verify(armRpoClient).getStorageAccounts(eq("Bearer refreshed"), any(StorageAccountRequest.class));
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getGetStorageAccountsRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getCompletedRpoStatus()), any());
    }

    @AfterEach
    void close() {
        armRpoHelperMocks.close();
    }

    private static @NotNull StorageAccountResponse getStorageAccountResponse() {
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
        return storageAccountResponse;
    }
}