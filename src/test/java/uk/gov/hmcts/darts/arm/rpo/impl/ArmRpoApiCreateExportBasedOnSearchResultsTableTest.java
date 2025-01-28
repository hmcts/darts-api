package uk.gov.hmcts.darts.arm.rpo.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.CreateExportBasedOnSearchResultsTableResponse;
import uk.gov.hmcts.darts.arm.component.ArmRpoDownloadProduction;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelperMocks;
import uk.gov.hmcts.darts.arm.model.rpo.MasterIndexFieldByRecordClassSchema;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ArmAutomatedTaskRepository;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArmRpoApiCreateExportBasedOnSearchResultsTableTest {

    private static final String PRODUCTION_NAME = "DARTS_RPO_2024-08-13";

    @Mock
    private ArmRpoClient armRpoClient;

    @Mock
    private ArmRpoService armRpoService;

    private ArmRpoApiImpl armRpoApi;

    private UserAccountEntity userAccount;
    private static final Integer EXECUTION_ID = 1;
    private static final ArmRpoHelperMocks ARM_RPO_HELPER_MOCKS = new ArmRpoHelperMocks();
    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity;

    @BeforeEach
    void setUp() {
        var armAutomatedTaskRepository = mock(ArmAutomatedTaskRepository.class);
        var currentTimeHelper = mock(CurrentTimeHelper.class);
        var armRpoDownloadProduction = mock(ArmRpoDownloadProduction.class);

        ArmApiConfigurationProperties armApiConfigurationProperties = new ArmApiConfigurationProperties();
        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        ObjectMapper objectMapper = objectMapperConfig.objectMapper();

        armRpoApi = new ArmRpoApiImpl(armRpoClient, armRpoService, armApiConfigurationProperties,
                                      armAutomatedTaskRepository, currentTimeHelper, armRpoDownloadProduction,
                                      objectMapper);

        armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(EXECUTION_ID);
        armRpoExecutionDetailEntity.setSearchId("searchId");
        armRpoExecutionDetailEntity.setSearchItemCount(7);
        armRpoExecutionDetailEntity.setProductionId("productionId");
        armRpoExecutionDetailEntity.setStorageAccountId("storageAccountId");
        when(armRpoService.getArmRpoExecutionDetailEntity(EXECUTION_ID)).thenReturn(armRpoExecutionDetailEntity);
    }

    @Test
    void createExportBasedOnSearchResultsTable_Success() {
        // given
        CreateExportBasedOnSearchResultsTableResponse response = new CreateExportBasedOnSearchResultsTableResponse();
        response.setStatus(200);
        response.setIsError(false);
        response.setResponseStatus(0);
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenReturn(response);

        // when
        boolean result = armRpoApi.createExportBasedOnSearchResultsTable(
            "token", 1, createHeaderColumns(), PRODUCTION_NAME, userAccount);

        // then
        assertTrue(result);
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void createExportBasedOnSearchResultsTable_ReturnsInProgress() {
        // given
        CreateExportBasedOnSearchResultsTableResponse response = new CreateExportBasedOnSearchResultsTableResponse();
        response.setStatus(400);
        response.setIsError(false);
        response.setResponseStatus(2);
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenReturn(response);

        // when
        boolean result = armRpoApi.createExportBasedOnSearchResultsTable(
            "token", 1, createHeaderColumns(), PRODUCTION_NAME, userAccount);

        // then
        assertFalse(result);
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void createExportBasedOnSearchResultsTable_ReturnsInProgress_WhenFeignExceptionThrownWithStatus400IsErrorFalseResponseStatus2() {
        // given
        FeignException feignException = mock(FeignException.class);
        when(feignException.contentUTF8()).thenReturn(getFeignResponseAsString("400", false, "2"));
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenThrow(feignException);

        // when
        boolean result = armRpoApi.createExportBasedOnSearchResultsTable(
            "token", 1, createHeaderColumns(), PRODUCTION_NAME, userAccount);

        // then
        assertFalse(result);
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WithStatus200IsErrorTrueResponseStatusZero() {
        // given
        CreateExportBasedOnSearchResultsTableResponse response = new CreateExportBasedOnSearchResultsTableResponse();
        response.setStatus(200);
        response.setIsError(true);
        response.setResponseStatus(0);
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenReturn(response);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            armRpoApi.createExportBasedOnSearchResultsTable("token", 1, createHeaderColumns(), PRODUCTION_NAME, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: ARM RPO API failed with status - 200 OK and response"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WhenFeignExceptionThrownWithStatus200IsErrorTrueResponseStatusZero() {
        // given
        FeignException feignException = mock(FeignException.class);
        when(feignException.contentUTF8()).thenReturn(getFeignResponseAsString("200", true, "0"));
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenThrow(feignException);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            armRpoApi.createExportBasedOnSearchResultsTable(
                "token", 1, createHeaderColumns(), PRODUCTION_NAME, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: ARM RPO API failed with status - 200 OK and response"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WithStatus400IsErrorTrueResponseStatusZero() {
        // given
        CreateExportBasedOnSearchResultsTableResponse response = new CreateExportBasedOnSearchResultsTableResponse();
        response.setStatus(400);
        response.setIsError(true);
        response.setResponseStatus(0);
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenReturn(response);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            armRpoApi.createExportBasedOnSearchResultsTable(
                "token", 1, createHeaderColumns(), PRODUCTION_NAME, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: ARM RPO API failed with invalid status - 400 BAD_REQUEST"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WhenFeignExceptionThrownWithStatus400IsErrorTrueResponseStatusZero() {
        // given
        FeignException feignException = mock(FeignException.class);
        when(feignException.contentUTF8()).thenReturn(getFeignResponseAsString("200", true, "0"));
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenThrow(feignException);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            armRpoApi.createExportBasedOnSearchResultsTable("token", 1, createHeaderColumns(), PRODUCTION_NAME, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: ARM RPO API failed with status - 200 OK and response"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WithStatus400IsErrorFalseResponseStatusZero() {
        // given
        CreateExportBasedOnSearchResultsTableResponse response = new CreateExportBasedOnSearchResultsTableResponse();
        response.setStatus(400);
        response.setIsError(false);
        response.setResponseStatus(0);
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenReturn(response);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            armRpoApi.createExportBasedOnSearchResultsTable("token", 1, createHeaderColumns(), PRODUCTION_NAME, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: ARM RPO API failed with invalid status - 400 BAD_REQUEST"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WhenFeignExceptionThrownWithStatus400IsErrorFalseResponseStatusZero() {
        // given
        FeignException feignException = mock(FeignException.class);
        when(feignException.contentUTF8()).thenReturn(getFeignResponseAsString("200", true, "0"));
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenThrow(feignException);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            armRpoApi.createExportBasedOnSearchResultsTable("token", 1, createHeaderColumns(), PRODUCTION_NAME, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: ARM RPO API failed with status - 200 OK and response"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WithStatus500IsErrorFalseResponseStatus500() {
        // given
        CreateExportBasedOnSearchResultsTableResponse response = new CreateExportBasedOnSearchResultsTableResponse();
        response.setStatus(500);
        response.setIsError(false);
        response.setResponseStatus(500);
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenReturn(response);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            armRpoApi.createExportBasedOnSearchResultsTable("token", 1, createHeaderColumns(), PRODUCTION_NAME, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: ARM RPO API failed with status - 500 INTERNAL_SERVER_ERROR"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WhenFeignExceptionThrownWithStatus500IsErrorFalseResponseStatus0() {
        // given
        FeignException feignException = mock(FeignException.class);
        when(feignException.contentUTF8()).thenReturn(getFeignResponseAsString("500", true, "0"));
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenThrow(feignException);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            armRpoApi.createExportBasedOnSearchResultsTable("token", 1, createHeaderColumns(), PRODUCTION_NAME, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: ARM RPO API failed with status - 500 INTERNAL_SERVER_ERROR and response"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WithInvalidRequest() {
        // given
        armRpoExecutionDetailEntity.setSearchId(null);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            armRpoApi.createExportBasedOnSearchResultsTable(
                "token", 1, createHeaderColumns(), PRODUCTION_NAME, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: Could not construct API request"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WhenClientThrowsFeignException() {
        // given
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenThrow(FeignException.class);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            armRpoApi.createExportBasedOnSearchResultsTable(
                "token", 1, createHeaderColumns(), PRODUCTION_NAME, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: Unable to get ARM RPO response"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WhenFeignExceptionThrownWithStatus500IsErrorFalseResponseStatus500() {
        // given
        FeignException feignException = mock(FeignException.class);
        when(feignException.contentUTF8()).thenReturn(getFeignResponseAsString("500", true, "500"));
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenThrow(feignException);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            armRpoApi.createExportBasedOnSearchResultsTable("token", 1, createHeaderColumns(), PRODUCTION_NAME, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: ARM RPO API failed with status - 500 INTERNAL_SERVER_ERROR"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WhenFeignExceptionThrownWithNullResponse() {
        // given
        FeignException feignException = mock(FeignException.class);
        when(feignException.contentUTF8()).thenReturn(null);
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenThrow(feignException);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            armRpoApi.createExportBasedOnSearchResultsTable("token", 1, createHeaderColumns(), PRODUCTION_NAME, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: Unable to get ARM RPO response from client"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WhenFeignExceptionThrownWithEmptyResponse() {
        // given
        FeignException feignException = mock(FeignException.class);
        when(feignException.contentUTF8()).thenReturn("");
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenThrow(feignException);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            armRpoApi.createExportBasedOnSearchResultsTable("token", 1, createHeaderColumns(), PRODUCTION_NAME, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: Unable to get ARM RPO response from client"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WhenFeignExceptionThrownWithInvalidJsonResponse() {
        // given
        FeignException feignException = mock(FeignException.class);
        when(feignException.contentUTF8()).thenReturn("{");
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenThrow(feignException);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            armRpoApi.createExportBasedOnSearchResultsTable(
                "token", 1, createHeaderColumns(), PRODUCTION_NAME, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: Unable to get ARM RPO response from client"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WhenFeignExceptionThrownWithEmptyResponseObject() {
        // given
        FeignException feignException = mock(FeignException.class);
        when(feignException.contentUTF8()).thenReturn("{}");
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenThrow(feignException);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            armRpoApi.createExportBasedOnSearchResultsTable(
                "token", 1, createHeaderColumns(), PRODUCTION_NAME, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: ARM RPO API createExportBasedOnSearchResultsTable is invalid"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    private String getFeignResponseAsString(String status, boolean isError, String responseStatus) {
        return "{\n"
            + "  \"status\": \"" + status + "\",\n"
            + "  \"isError\": " + isError + ",\n"
            + "  \"responseStatus\": \"" + responseStatus + "\"\n"
            + "}";
    }

    private List<MasterIndexFieldByRecordClassSchema> createHeaderColumns() {
        return List.of(
            createMasterIndexFieldByRecordClassSchema("200b9c27-b497-4977-82e7-1586b32a5871", "Record Class", "record_class", "string", false),
            createMasterIndexFieldByRecordClassSchema("90ee0e13-8639-4c4a-b542-66b6c8911549", "Archived Date", "ingestionDate", "date", false),
            createMasterIndexFieldByRecordClassSchema("a9b8daf2-d9ff-4815-b65a-f6ae2763b92c", "Client Identifier", "client_identifier", "string",
                                                      false),
            createMasterIndexFieldByRecordClassSchema("109b6bf1-57a0-48ec-b22e-c7248dc74f91", "Contributor", "contributor", "string", false),
            createMasterIndexFieldByRecordClassSchema("893048bf-1e7c-4811-9abf-00cd77a715cf", "Record Date", "recordDate", "date", false),
            createMasterIndexFieldByRecordClassSchema("fdd0fcbb-da46-4af1-a627-ac255c12bb23", "ObjectId", "bf_012", "number", true)
        );
    }

    private MasterIndexFieldByRecordClassSchema createMasterIndexFieldByRecordClassSchema(
        String uuid, String displayName, String propertyName, String propertyType, boolean isMasked) {

        return MasterIndexFieldByRecordClassSchema.builder()
            .masterIndexField(uuid)
            .displayName(displayName)
            .propertyName(propertyName)
            .propertyType(propertyType)
            .isMasked(isMasked)
            .build();

    }

    @AfterAll
    static void close() {
        ARM_RPO_HELPER_MOCKS.close();
    }
}