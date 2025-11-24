package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.RemoveProductionRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.RemoveProductionResponse;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoveProductionServiceTest {

    private static final Integer EXECUTION_ID = 1;
    private static final String BEARER_TOKEN = "token";

    @Mock
    private ArmRpoClient armRpoClient;
    @Mock
    private ArmApiService armApiService;
    @Mock
    private ArmRpoService armRpoService;

    private ArmRpoUtil armRpoUtil;

    private RemoveProductionServiceImpl removeProductionService;

    @Captor
    private ArgumentCaptor<ArmRpoExecutionDetailEntity> armRpoExecutionDetailEntityArgumentCaptor;

    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity;
    private UserAccountEntity userAccount;

    private ArmRpoHelperMocks armRpoHelperMocks;

    @BeforeEach
    void setUp() {
        armRpoHelperMocks = new ArmRpoHelperMocks();
        armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(EXECUTION_ID);
        userAccount = new UserAccountEntity();
        armRpoExecutionDetailEntityArgumentCaptor = ArgumentCaptor.forClass(ArmRpoExecutionDetailEntity.class);
        armRpoUtil = spy(new ArmRpoUtil(armRpoService, armApiService));
        ArmClientService armClientService = new ArmClientServiceImpl(null, null, armRpoClient);
        removeProductionService = new RemoveProductionServiceImpl(armClientService, armRpoService, armRpoUtil);
    }

    @Test
    void removeProduction_Success() {
        // given
        RemoveProductionResponse response = new RemoveProductionResponse();
        response.setStatus(200);
        response.setIsError(false);

        armRpoExecutionDetailEntity.setProductionId("123");
        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.removeProduction(anyString(), any(RemoveProductionRequest.class))).thenReturn(response);

        // when
        removeProductionService.removeProduction(BEARER_TOKEN, EXECUTION_ID, userAccount);

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntityArgumentCaptor.capture(),
                                                         eq(armRpoHelperMocks.getRemoveProductionRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(eq(armRpoExecutionDetailEntity), eq(armRpoHelperMocks.getCompletedRpoStatus()), eq(userAccount));
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void removeProduction_ThrowsFeignException() {
        // given
        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.removeProduction(anyString(), any(RemoveProductionRequest.class))).thenThrow(FeignException.class);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () -> removeProductionService.removeProduction("token", 1, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM RPO removeProduction: Unable to get ARM RPO response"));
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntityArgumentCaptor.capture(),
                                                         eq(armRpoHelperMocks.getRemoveProductionRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity, armRpoHelperMocks.getFailedRpoStatus(), userAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void removeProduction_WithNullResponse() {
        // given
        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.removeProduction(anyString(), any(RemoveProductionRequest.class))).thenReturn(null);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () -> removeProductionService.removeProduction("token", 1, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM RPO removeProduction: ARM RPO API response is invalid"));
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntityArgumentCaptor.capture(),
                                                         eq(armRpoHelperMocks.getRemoveProductionRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(eq(armRpoExecutionDetailEntity), eq(armRpoHelperMocks.getFailedRpoStatus()), eq(userAccount));
        verifyNoMoreInteractions(armRpoService);
    }

    /**
     * Add to RemoveProductionServiceTest.java
     */
    @Test
    void removeProduction_shouldRetryOn401_thenSucceed() {
        // given
        Response response = Response.builder()
            .request(Request.create(Request.HttpMethod.POST, "/some", java.util.Map.of(), null, StandardCharsets.UTF_8, null))
            .status(401)
            .reason("Unauthorized")
            .build();
        FeignException feign401 = FeignException.errorStatus("removeProduction", response);

        // First call throws 401
        when(armRpoClient.removeProduction(eq(BEARER_TOKEN), any(RemoveProductionRequest.class))).thenThrow(feign401);

        // armRpoUtil should be asked for a new token
        doReturn("Bearer refreshed").when(armRpoUtil).retryGetBearerToken(anyString());

        RemoveProductionResponse removeProductionResponse = new RemoveProductionResponse();
        removeProductionResponse.setStatus(200);
        removeProductionResponse.setIsError(false);
        when(armRpoClient.removeProduction(eq("Bearer refreshed"), any(RemoveProductionRequest.class))).thenReturn(removeProductionResponse);

        armRpoExecutionDetailEntity.setProductionId("123");
        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);

        // when
        removeProductionService.removeProduction(BEARER_TOKEN, 1, userAccount);

        // then
        verify(armRpoClient).removeProduction(eq(BEARER_TOKEN), any(RemoveProductionRequest.class));
        verify(armRpoUtil).retryGetBearerToken(anyString());
        verify(armRpoClient).removeProduction(eq("Bearer refreshed"), any(RemoveProductionRequest.class));

    }

    @Test
    void removeProduction_shouldRetryOn403_thenSucceed() {
        // given
        Response response = Response.builder()
            .request(Request.create(Request.HttpMethod.POST, "/removeProduction", java.util.Map.of(), null, StandardCharsets.UTF_8, null))
            .status(403)
            .reason("Forbidden")
            .build();
        FeignException feign403 = FeignException.errorStatus("removeProduction", response);

        when(armRpoClient.removeProduction(eq(BEARER_TOKEN), any(RemoveProductionRequest.class)))
            .thenThrow(feign403);

        doReturn("Bearer refreshed").when(armRpoUtil).retryGetBearerToken(anyString());

        RemoveProductionResponse removeProductionResponse = new RemoveProductionResponse();
        removeProductionResponse.setStatus(200);
        removeProductionResponse.setIsError(false);
        when(armRpoClient.removeProduction(eq("Bearer refreshed"), any(RemoveProductionRequest.class)))
            .thenReturn(removeProductionResponse);

        armRpoExecutionDetailEntity.setProductionId("123");
        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);

        // when
        removeProductionService.removeProduction(BEARER_TOKEN, 1, userAccount);

        // then
        verify(armRpoClient).removeProduction(eq(BEARER_TOKEN), any(RemoveProductionRequest.class));
        verify(armRpoUtil).retryGetBearerToken("removeProduction");
        verify(armRpoClient).removeProduction(eq("Bearer refreshed"), any(RemoveProductionRequest.class));
    }


    @AfterEach
    void close() {
        armRpoHelperMocks.close();
    }
}