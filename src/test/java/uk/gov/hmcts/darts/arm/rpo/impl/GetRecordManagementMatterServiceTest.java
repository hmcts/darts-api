package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.EmptyRpoRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.RecordManagementMatterResponse;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetRecordManagementMatterServiceTest {

    private static final Integer EXECUTION_ID = 1;
    public static final String BEARER_TOKEN = "token";

    @Mock
    private ArmRpoClient armRpoClient;
    @Mock
    private ArmApiService armApiService;
    @Mock
    private ArmRpoService armRpoService;

    private ArmRpoUtil armRpoUtil;

    private GetRecordManagementMatterServiceImpl getRecordManagementMatterService;

    private UserAccountEntity userAccountEntity;

    private ArmRpoHelperMocks armRpoHelperMocks;

    @BeforeEach
    void setUp() {
        armRpoHelperMocks = new ArmRpoHelperMocks();
        userAccountEntity = new UserAccountEntity();

        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(EXECUTION_ID);
        when(armRpoService.getArmRpoExecutionDetailEntity(EXECUTION_ID)).thenReturn(armRpoExecutionDetailEntity);
        armRpoUtil = spy(new ArmRpoUtil(armRpoService, armApiService));
        ArmClientService armClientService = new ArmClientServiceImpl(null, null, armRpoClient);
        getRecordManagementMatterService = new GetRecordManagementMatterServiceImpl(armClientService, armRpoService, armRpoUtil);
    }

    @Test
    void getRecordManagementMatter_ThrowsException_WhenResponseReturnedWithoutMatterId() {
        // given
        RecordManagementMatterResponse expectedResponse = new RecordManagementMatterResponse();
        expectedResponse.setStatus(200);
        expectedResponse.setIsError(false);
        when(armRpoClient.getRecordManagementMatter(anyString(), any())).thenReturn(expectedResponse);

        // when
        assertThrows(ArmRpoException.class, () -> getRecordManagementMatterService.getRecordManagementMatter(BEARER_TOKEN, EXECUTION_ID, userAccountEntity));

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getGetRecordManagementMatterRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
    }

    @Test
    void getRecordManagementMatter_ThrowsArmRpoException_WhenClientFails() {
        // given
        when(armRpoClient.getRecordManagementMatter(anyString(), any())).thenThrow(FeignException.class);

        // when
        assertThrows(ArmRpoException.class, () -> getRecordManagementMatterService.getRecordManagementMatter(BEARER_TOKEN, EXECUTION_ID, userAccountEntity));

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getGetRecordManagementMatterRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
    }

    @Test
    void getRecordManagementMatter_UpdatesStatusToInProgress() {
        // when
        assertThrows(ArmRpoException.class, () -> getRecordManagementMatterService.getRecordManagementMatter(BEARER_TOKEN, EXECUTION_ID, userAccountEntity));

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getGetRecordManagementMatterRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
    }

    @Test
    void getRecordManagementMatter_SetsMatterId_WhenResponseIsValid() {
        // given
        RecordManagementMatterResponse response = new RecordManagementMatterResponse();
        response.setStatus(200);
        response.setIsError(false);
        response.setRecordManagementMatter(new RecordManagementMatterResponse.RecordManagementMatter());
        response.getRecordManagementMatter().setMatterId("123");
        when(armRpoClient.getRecordManagementMatter(anyString(), any())).thenReturn(response);

        Integer executionId = 1;
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(executionId);
        when(armRpoService.getArmRpoExecutionDetailEntity(executionId)).thenReturn(armRpoExecutionDetailEntity);

        // when
        getRecordManagementMatterService.getRecordManagementMatter(BEARER_TOKEN, executionId, userAccountEntity);

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getGetRecordManagementMatterRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getCompletedRpoStatus()), any());
    }

    @Test
    void getRecordManagementMatter_ShouldRetryOnUnauthorised_WhenResponseIsValid() {
        // given
        Response response = Response.builder()
            .request(Request.create(Request.HttpMethod.POST, "/getRecordManagementMatter", java.util.Map.of(), null, StandardCharsets.UTF_8, null))
            .status(401)
            .reason("Unauthorized")
            .build();
        FeignException feign401 = FeignException.errorStatus("getRecordManagementMatter", response);

        // First call throws 401
        when(armRpoClient.getRecordManagementMatter(eq(BEARER_TOKEN), any(EmptyRpoRequest.class))).thenThrow(feign401);

        // armRpoUtil should be asked for a new token
        doReturn("Bearer refreshed").when(armRpoUtil).retryGetBearerToken(anyString());

        RecordManagementMatterResponse recordManagementMatterResponse = new RecordManagementMatterResponse();
        recordManagementMatterResponse.setStatus(200);
        recordManagementMatterResponse.setIsError(false);
        recordManagementMatterResponse.setRecordManagementMatter(new RecordManagementMatterResponse.RecordManagementMatter());
        recordManagementMatterResponse.getRecordManagementMatter().setMatterId("123");
        when(armRpoClient.getRecordManagementMatter(eq("Bearer refreshed"), any())).thenReturn(recordManagementMatterResponse);

        Integer executionId = 1;
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(executionId);
        when(armRpoService.getArmRpoExecutionDetailEntity(executionId)).thenReturn(armRpoExecutionDetailEntity);

        // when
        getRecordManagementMatterService.getRecordManagementMatter(BEARER_TOKEN, executionId, userAccountEntity);

        // then
        verify(armRpoClient).getRecordManagementMatter(eq(BEARER_TOKEN), any());
        verify(armRpoUtil).retryGetBearerToken("getRecordManagementMatter");
        verify(armRpoClient).getRecordManagementMatter(eq("Bearer refreshed"), any());
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getGetRecordManagementMatterRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getCompletedRpoStatus()), any());
    }

    @Test
    void getRecordManagementMatter_ShouldRetryOnForbidden_WhenResponseIsValid() {
        // given
        Response response = Response.builder()
            .request(Request.create(Request.HttpMethod.POST, "/getRecordManagementMatter", java.util.Map.of(), null, StandardCharsets.UTF_8, null))
            .status(403)
            .reason("Forbidden")
            .build();
        FeignException feign401 = FeignException.errorStatus("getRecordManagementMatter", response);

        // First call throws 401
        when(armRpoClient.getRecordManagementMatter(eq(BEARER_TOKEN), any(EmptyRpoRequest.class))).thenThrow(feign401);

        // armRpoUtil should be asked for a new token
        doReturn("Bearer refreshed").when(armRpoUtil).retryGetBearerToken(anyString());

        RecordManagementMatterResponse recordManagementMatterResponse = new RecordManagementMatterResponse();
        recordManagementMatterResponse.setStatus(200);
        recordManagementMatterResponse.setIsError(false);
        recordManagementMatterResponse.setRecordManagementMatter(new RecordManagementMatterResponse.RecordManagementMatter());
        recordManagementMatterResponse.getRecordManagementMatter().setMatterId("123");
        when(armRpoClient.getRecordManagementMatter(eq("Bearer refreshed"), any())).thenReturn(recordManagementMatterResponse);

        Integer executionId = 1;
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(executionId);
        when(armRpoService.getArmRpoExecutionDetailEntity(executionId)).thenReturn(armRpoExecutionDetailEntity);

        // when
        getRecordManagementMatterService.getRecordManagementMatter(BEARER_TOKEN, executionId, userAccountEntity);

        // then
        verify(armRpoClient).getRecordManagementMatter(eq(BEARER_TOKEN), any());
        verify(armRpoUtil).retryGetBearerToken("getRecordManagementMatter");
        verify(armRpoClient).getRecordManagementMatter(eq("Bearer refreshed"), any());
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getGetRecordManagementMatterRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getCompletedRpoStatus()), any());
    }

    @AfterEach
    void close() {
        armRpoHelperMocks.close();
    }
}