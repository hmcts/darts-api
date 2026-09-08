package uk.gov.hmcts.darts.arm.rpo.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import uk.gov.hmcts.darts.arm.client.model.rpo.SaveBackgroundSearchRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.SaveBackgroundSearchResponse;
import uk.gov.hmcts.darts.arm.client.version.fivetwo.ArmApiBaseClient;
import uk.gov.hmcts.darts.arm.client.version.fivetwo.ArmAuthClient;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.exception.ArmRpoSearchNoResultsException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelperMocks;
import uk.gov.hmcts.darts.arm.service.ArmApiService;
import uk.gov.hmcts.darts.arm.service.ArmClientService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.service.impl.ArmClientServiceWrapper;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.nio.charset.StandardCharsets;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.CloseResource"})
@ConditionalOnProperty(prefix = "darts.storage.arm-api", name = "enable-arm-v5-2-upgrade", havingValue = "true")
class SaveBackgroundSearchServiceTest {

    private static final Integer EXECUTION_ID = 1;

    @Mock
    private ArmAuthClient armAuthClient;
    @Mock
    private ArmApiBaseClient armApiBaseClient;

    @Mock
    private ArmRpoService armRpoService;
    @Mock
    private ArmApiService armApiService;

    private ArmRpoUtil armRpoUtil;
    private SaveBackgroundSearchServiceImpl saveBackgroundSearchService;

    private UserAccountEntity userAccount;
    private ArmRpoHelperMocks armRpoHelperMocks;

    @BeforeEach
    void setUp() {
        armRpoHelperMocks = new ArmRpoHelperMocks();
        userAccount = new UserAccountEntity();

        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(EXECUTION_ID);
        when(armRpoService.getArmRpoExecutionDetailEntity(EXECUTION_ID)).thenReturn(armRpoExecutionDetailEntity);
        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        ObjectMapper objectMapper = objectMapperConfig.objectMapper();

        armRpoUtil = spy(new ArmRpoUtil(armRpoService, armApiService));
        ArmClientService armClientService = new ArmClientServiceWrapper(armAuthClient, armApiBaseClient);
        saveBackgroundSearchService = new SaveBackgroundSearchServiceImpl(armClientService, armRpoService, armRpoUtil, objectMapper);
    }

    @Test
    void saveBackgroundSearch_ReturnsSuccess() {
        // given
        SaveBackgroundSearchResponse saveBackgroundSearchResponse = new SaveBackgroundSearchResponse();
        saveBackgroundSearchResponse.setStatus(200);
        saveBackgroundSearchResponse.setIsError(false);
        when(armApiBaseClient.saveBackgroundSearch(anyString(), any())).thenReturn(saveBackgroundSearchResponse);

        // when
        saveBackgroundSearchService.saveBackgroundSearch("token", 1, "searchName", userAccount);

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getSaveBackgroundSearchRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getCompletedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void saveBackgroundSearch_ReturnsInvalidStatus() {
        // given
        SaveBackgroundSearchResponse saveBackgroundSearchResponse = new SaveBackgroundSearchResponse();
        saveBackgroundSearchResponse.setStatus(400);
        saveBackgroundSearchResponse.setIsError(true);
        when(armApiBaseClient.saveBackgroundSearch(anyString(), any())).thenReturn(saveBackgroundSearchResponse);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () -> saveBackgroundSearchService.saveBackgroundSearch(
            "token", 1, "searchName", userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM save background search: ARM RPO API failed with status - 400 BAD_REQUEST and response"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getSaveBackgroundSearchRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void saveBackgroundSearch_ThrowsFeignException() {
        // given
        when(armApiBaseClient.saveBackgroundSearch(anyString(), any(SaveBackgroundSearchRequest.class)))
            .thenThrow(FeignException.class);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () -> saveBackgroundSearchService.saveBackgroundSearch(
            "token", 1, "searchName", userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM save background search: Unable to save background search"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getSaveBackgroundSearchRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void saveBackgroundSearch_ReturnsNullResponse() {
        // given
        when(armApiBaseClient.saveBackgroundSearch(anyString(), any(SaveBackgroundSearchRequest.class))).thenReturn(null);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () -> saveBackgroundSearchService.saveBackgroundSearch(
            "token", 1, "searchName", userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM save background search: ARM RPO API response is invalid - null"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getSaveBackgroundSearchRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void saveBackgroundSearch_ReturnsNullStatusHttpResponse() {
        // given
        SaveBackgroundSearchResponse saveBackgroundSearchResponse = new SaveBackgroundSearchResponse();
        saveBackgroundSearchResponse.setIsError(true);
        when(armApiBaseClient.saveBackgroundSearch(anyString(), any(SaveBackgroundSearchRequest.class))).thenReturn(null);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () -> saveBackgroundSearchService.saveBackgroundSearch(
            "token", 1, "searchName", userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM save background search: ARM RPO API response is invalid"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getSaveBackgroundSearchRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void saveBackgroundSearch_ReturnsNullIsErrorResponse() {
        // given
        SaveBackgroundSearchResponse saveBackgroundSearchResponse = new SaveBackgroundSearchResponse();
        saveBackgroundSearchResponse.setStatus(200);
        when(armApiBaseClient.saveBackgroundSearch(anyString(), any(SaveBackgroundSearchRequest.class))).thenReturn(null);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () -> saveBackgroundSearchService.saveBackgroundSearch(
            "token", 1, "searchName", userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM save background search: ARM RPO API response is invalid"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getSaveBackgroundSearchRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void saveBackgroundSearch_ReturnsInvalidHttpStatusResponse() {
        // given
        SaveBackgroundSearchResponse saveBackgroundSearchResponse = new SaveBackgroundSearchResponse();
        saveBackgroundSearchResponse.setStatus(-1);
        when(armApiBaseClient.saveBackgroundSearch(anyString(), any(SaveBackgroundSearchRequest.class))).thenReturn(null);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () -> saveBackgroundSearchService.saveBackgroundSearch(
            "token", 1, "searchName", userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM save background search: ARM RPO API response is invalid"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getSaveBackgroundSearchRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void saveBackgroundSearch_ThrowsFeignBadRequestExceptionWithMessage() {
        // given
        FeignException feignException = FeignException.errorStatus(
            "saveBackgroundSearch",
            Response.builder()
                .status(400)
                .reason("Bad Request")
                .request(Request.create(Request.HttpMethod.POST, "/saveBackgroundSearch", emptyMap(), null, null, null))
                .body("Search with no results", StandardCharsets.UTF_8)
                .build()
        );
        when(armApiBaseClient.saveBackgroundSearch(anyString(), any(SaveBackgroundSearchRequest.class)))
            .thenThrow(feignException);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            saveBackgroundSearchService.saveBackgroundSearch("token", 1, "searchName", userAccount)
        );

        // then
        assertThat(armRpoException.getMessage(), containsString("Search with no results"));
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getSaveBackgroundSearchRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()), any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void saveBackgroundSearch_ThrowsFeignBadRequestExceptionWithJsonMessage() {
        // given
        String jsonResponse = "{\"status\":400,\"isError\":true,\"responseStatus\":1,\"message\":\"Search with no results cannot be saved\"}";
        FeignException feignException = FeignException.errorStatus(
            "saveBackgroundSearch",
            Response.builder()
                .status(400)
                .reason("Bad Request")
                .request(Request.create(Request.HttpMethod.POST, "/saveBackgroundSearch", emptyMap(), null, null, null))
                .body(jsonResponse, StandardCharsets.UTF_8)
                .build()
        );
        when(armApiBaseClient.saveBackgroundSearch(anyString(), any(SaveBackgroundSearchRequest.class)))
            .thenThrow(feignException);

        // when
        ArmRpoSearchNoResultsException armRpoException = assertThrows(ArmRpoSearchNoResultsException.class, () ->
            saveBackgroundSearchService.saveBackgroundSearch("token", 1, "searchName", userAccount)
        );

        // then
        assertThat(armRpoException.getMessage(), containsString("RPO endpoint search returned no results for execution id 1"));
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getSaveBackgroundSearchRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()), any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void saveBackgroundSearch_ThrowsFeignExceptionWithJsonMessage() {
        // given
        String jsonResponse = "{\"status\":500,\"isError\":true,\"responseStatus\":1}";
        FeignException feignException = FeignException.errorStatus(
            "saveBackgroundSearch",
            Response.builder()
                .status(500)
                .reason("Bad Request")
                .request(Request.create(Request.HttpMethod.POST, "/saveBackgroundSearch", emptyMap(), null, null, null))
                .body(jsonResponse, StandardCharsets.UTF_8)
                .build()
        );
        when(armApiBaseClient.saveBackgroundSearch(anyString(), any(SaveBackgroundSearchRequest.class)))
            .thenThrow(feignException);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            saveBackgroundSearchService.saveBackgroundSearch("token", 1, "searchName", userAccount)
        );

        // then
        assertThat(armRpoException.getMessage(), containsString("Failure during ARM save background search: Unable to save background search"));
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getSaveBackgroundSearchRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()), any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void saveBackgroundSearch_shouldRetryOnUnauthorised_thenSucceed() {
        // given
        Response response = Response.builder()
            .request(Request.create(Request.HttpMethod.POST, "/saveBackgroundSearch", java.util.Map.of(), null, StandardCharsets.UTF_8, null))
            .status(401)
            .reason("Unauthorized")
            .build();
        FeignException feign401 = FeignException.errorStatus("saveBackgroundSearch", response);

        // First call throws 401
        when(armApiBaseClient.saveBackgroundSearch(eq("token"), any(SaveBackgroundSearchRequest.class))).thenThrow(feign401);

        // armRpoUtil should be asked for a new token
        doReturn("Bearer refreshed").when(armRpoUtil).retryGetBearerToken(anyString());

        SaveBackgroundSearchResponse saveBackgroundSearchResponse = new SaveBackgroundSearchResponse();
        saveBackgroundSearchResponse.setStatus(200);
        saveBackgroundSearchResponse.setIsError(false);
        when(armApiBaseClient.saveBackgroundSearch(eq("Bearer refreshed"), any(SaveBackgroundSearchRequest.class))).thenReturn(saveBackgroundSearchResponse);

        // when
        saveBackgroundSearchService.saveBackgroundSearch("token", 1, "searchName", userAccount);

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getSaveBackgroundSearchRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getCompletedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void saveBackgroundSearch_shouldRetryOnUnauthorised_thenFailSecondUnauthorised() {
        // given
        Response response = Response.builder()
            .request(Request.create(Request.HttpMethod.POST, "/saveBackgroundSearch", java.util.Map.of(), null, StandardCharsets.UTF_8, null))
            .status(401)
            .reason("Unauthorized")
            .build();
        FeignException feign401 = FeignException.errorStatus("saveBackgroundSearch", response);

        // First call throws 401
        when(armApiBaseClient.saveBackgroundSearch(eq("token"), any(SaveBackgroundSearchRequest.class))).thenThrow(feign401);

        // armRpoUtil should be asked for a new token
        doReturn("Bearer refreshed").when(armRpoUtil).retryGetBearerToken(anyString());

        when(armApiBaseClient.saveBackgroundSearch(eq("Bearer refreshed"), any(SaveBackgroundSearchRequest.class))).thenThrow(feign401);

        // when
        ArmRpoException exception = assertThrows(ArmRpoException.class, () ->
            saveBackgroundSearchService.saveBackgroundSearch("token", 1, "searchName", userAccount));

        // then
        assertThat(exception.getMessage(), containsString("Unauthorized"));
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getSaveBackgroundSearchRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void saveBackgroundSearch_shouldRetryOnForbidden_thenSucceed() {
        // given
        Response response = Response.builder()
            .request(Request.create(Request.HttpMethod.POST, "/saveBackgroundSearch", java.util.Map.of(), null, StandardCharsets.UTF_8, null))
            .status(403)
            .reason("Forbidden")
            .build();
        FeignException feign403 = FeignException.errorStatus("saveBackgroundSearch", response);

        // First call throws 403
        when(armApiBaseClient.saveBackgroundSearch(eq("token"), any(SaveBackgroundSearchRequest.class))).thenThrow(feign403);

        // armRpoUtil should be asked for a new token
        doReturn("Bearer refreshed").when(armRpoUtil).retryGetBearerToken(anyString());

        SaveBackgroundSearchResponse saveBackgroundSearchResponse = new SaveBackgroundSearchResponse();
        saveBackgroundSearchResponse.setStatus(200);
        saveBackgroundSearchResponse.setIsError(false);
        when(armApiBaseClient.saveBackgroundSearch(eq("Bearer refreshed"), any(SaveBackgroundSearchRequest.class))).thenReturn(saveBackgroundSearchResponse);

        // when
        saveBackgroundSearchService.saveBackgroundSearch("token", 1, "searchName", userAccount);

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getSaveBackgroundSearchRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getCompletedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @AfterEach
    void close() {
        armRpoHelperMocks.close();
    }

}