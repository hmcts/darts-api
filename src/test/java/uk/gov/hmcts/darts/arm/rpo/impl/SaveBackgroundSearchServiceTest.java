package uk.gov.hmcts.darts.arm.rpo.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.SaveBackgroundSearchRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.SaveBackgroundSearchResponse;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.exception.ArmRpoSearchNoResultsException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelperMocks;
import uk.gov.hmcts.darts.arm.service.ArmClientService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.service.impl.ArmClientServiceImpl;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveBackgroundSearchServiceTest {

    @Mock
    private ArmRpoClient armRpoClient;

    @Mock
    private ArmRpoService armRpoService;

    private SaveBackgroundSearchServiceImpl saveBackgroundSearchService;

    private UserAccountEntity userAccount;
    private static final Integer EXECUTION_ID = 1;
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

        ArmRpoUtil armRpoUtil = new ArmRpoUtil(armRpoService);
        ArmClientService armClientService = new ArmClientServiceImpl(null, null, armRpoClient);
        saveBackgroundSearchService = new SaveBackgroundSearchServiceImpl(armClientService, armRpoService, armRpoUtil, objectMapper);
    }

    @Test
    void saveBackgroundSearch_ReturnsSuccess() {
        // given
        SaveBackgroundSearchResponse saveBackgroundSearchResponse = new SaveBackgroundSearchResponse();
        saveBackgroundSearchResponse.setStatus(200);
        saveBackgroundSearchResponse.setIsError(false);
        when(armRpoClient.saveBackgroundSearch(anyString(), any())).thenReturn(saveBackgroundSearchResponse);

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
        when(armRpoClient.saveBackgroundSearch(anyString(), any())).thenReturn(saveBackgroundSearchResponse);

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
        when(armRpoClient.saveBackgroundSearch(anyString(), any(SaveBackgroundSearchRequest.class)))
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
        when(armRpoClient.saveBackgroundSearch(anyString(), any(SaveBackgroundSearchRequest.class))).thenReturn(null);

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
        when(armRpoClient.saveBackgroundSearch(anyString(), any(SaveBackgroundSearchRequest.class))).thenReturn(null);

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
        when(armRpoClient.saveBackgroundSearch(anyString(), any(SaveBackgroundSearchRequest.class))).thenReturn(null);

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
        when(armRpoClient.saveBackgroundSearch(anyString(), any(SaveBackgroundSearchRequest.class))).thenReturn(null);

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
            feign.Response.builder()
                .status(400)
                .reason("Bad Request")
                .request(feign.Request.create(feign.Request.HttpMethod.POST, "/saveBackgroundSearch", emptyMap(), null, null, null))
                .body("Search with no results", StandardCharsets.UTF_8)
                .build()
        );
        when(armRpoClient.saveBackgroundSearch(anyString(), any(SaveBackgroundSearchRequest.class)))
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
            feign.Response.builder()
                .status(400)
                .reason("Bad Request")
                .request(feign.Request.create(feign.Request.HttpMethod.POST, "/saveBackgroundSearch", emptyMap(), null, null, null))
                .body(jsonResponse, StandardCharsets.UTF_8)
                .build()
        );
        when(armRpoClient.saveBackgroundSearch(anyString(), any(SaveBackgroundSearchRequest.class)))
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
            feign.Response.builder()
                .status(500)
                .reason("Bad Request")
                .request(feign.Request.create(feign.Request.HttpMethod.POST, "/saveBackgroundSearch", emptyMap(), null, null, null))
                .body(jsonResponse, StandardCharsets.UTF_8)
                .build()
        );
        when(armRpoClient.saveBackgroundSearch(anyString(), any(SaveBackgroundSearchRequest.class)))
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

    @AfterEach
    void close() {
        armRpoHelperMocks.close();
    }

}