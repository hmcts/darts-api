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
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.CreateExportBasedOnSearchResultsTableResponse;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelperMocks;
import uk.gov.hmcts.darts.arm.model.rpo.MasterIndexFieldByRecordClassSchema;
import uk.gov.hmcts.darts.arm.rpo.CreateExportBasedOnSearchResultsTableService;
import uk.gov.hmcts.darts.arm.service.ArmApiService;
import uk.gov.hmcts.darts.arm.service.ArmClientService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.service.impl.ArmClientServiceImpl;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.CloseResource"})
class CreateExportBasedOnSearchResultsTableServiceTest {

    private static final String PRODUCTION_NAME = "DARTS_RPO_2024-08-13";
    private static final String BEARER_TOKEN = "token";
    private static final Integer EXECUTION_ID = 1;

    @Mock
    private ArmRpoClient armRpoClient;
    @Mock
    private ArmApiService armApiService;
    @Mock
    private ArmRpoService armRpoService;

    private CreateExportBasedOnSearchResultsTableService createExportBasedOnSearchResultsTableCheckService;

    @Mock
    private UserAccountEntity userAccount;
    @Mock
    private CurrentTimeHelper currentTimeHelper;

    private ArmRpoHelperMocks armRpoHelperMocks;
    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity;
    private final Duration pollDuration = Duration.ofHours(4);
    private ArmRpoUtil armRpoUtil;

    @BeforeEach
    void setUp() {
        armRpoHelperMocks = new ArmRpoHelperMocks();

        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        ObjectMapper objectMapper = objectMapperConfig.objectMapper();

        armRpoUtil = spy(new ArmRpoUtil(armRpoService, armApiService));

        ArmClientService armClientService = new ArmClientServiceImpl(null, null, armRpoClient);

        createExportBasedOnSearchResultsTableCheckService = new CreateExportBasedOnSearchResultsTableServiceImpl(
            armClientService, armRpoService, armRpoUtil, currentTimeHelper, objectMapper);

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
        CreateExportBasedOnSearchResultsTableResponse response = createResponse(200, false, 0);
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenReturn(response);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();

        // when
        boolean result = createExportBasedOnSearchResultsTableCheckService.createExportBasedOnSearchResultsTable(
            BEARER_TOKEN, 1, headerColumns, PRODUCTION_NAME, pollDuration, userAccount);

        // then
        assertTrue(result);
        // assert that the productionName of armRpoExecutionDetailEntity contains the production name
        assertThat(armRpoExecutionDetailEntity.getProductionName(), containsString(PRODUCTION_NAME));
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getCompletedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void createExportBasedOnSearchResultsTable_ReturnsInProgress() {
        // given
        CreateExportBasedOnSearchResultsTableResponse response = createResponse(400, false, 2);
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenReturn(response);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();

        // when
        boolean result = createExportBasedOnSearchResultsTableCheckService.createExportBasedOnSearchResultsTable(
            BEARER_TOKEN, 1, headerColumns, PRODUCTION_NAME, pollDuration, userAccount);

        // then
        assertFalse(result);
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void createExportBasedOnSearchResultsTable_ReturnsInProgress_WithPollingCreatedTimestampInRange() {
        // given
        CreateExportBasedOnSearchResultsTableResponse response = createResponse(400, false, 2);
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenReturn(response);
        armRpoExecutionDetailEntity.setPollingCreatedAt(OffsetDateTime.now());
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.now());
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();

        // when
        boolean result = createExportBasedOnSearchResultsTableCheckService.createExportBasedOnSearchResultsTable(
            BEARER_TOKEN, 1, headerColumns, PRODUCTION_NAME, pollDuration, userAccount);

        // then
        assertFalse(result);
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WhenPollingCreatedTimestampOutOfRange() {
        // given
        CreateExportBasedOnSearchResultsTableResponse response = createResponse(400, false, 2);
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenReturn(response);
        armRpoExecutionDetailEntity.setPollingCreatedAt(OffsetDateTime.now().minusHours(5));
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.now());
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            createExportBasedOnSearchResultsTableCheckService.createExportBasedOnSearchResultsTable(
                BEARER_TOKEN, 1, headerColumns, PRODUCTION_NAME, pollDuration, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: Polling can only run for a maximum of"));
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void createExportBasedOnSearchResultsTable_ReturnsInProgress_WhenFeignExceptionThrownWithStatus400IsErrorFalseResponseStatus2() {
        // given
        FeignException feignException = mock(FeignException.class);
        when(feignException.contentUTF8()).thenReturn(getFeignResponseAsString("400", false, "2"));
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenThrow(feignException);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();

        // when
        boolean result = createExportBasedOnSearchResultsTableCheckService.createExportBasedOnSearchResultsTable(
            BEARER_TOKEN, 1, headerColumns, PRODUCTION_NAME, pollDuration, userAccount);

        // then
        assertFalse(result);
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WithStatus200IsErrorTrueResponseStatusZero() {
        // given
        CreateExportBasedOnSearchResultsTableResponse response = createResponse(200, true, 0);
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenReturn(response);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            createExportBasedOnSearchResultsTableCheckService.createExportBasedOnSearchResultsTable(BEARER_TOKEN, 1, headerColumns, PRODUCTION_NAME,
                                                                                                    pollDuration, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: ARM RPO API failed with status - 200 OK and response"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WhenFeignExceptionThrownWithStatus200IsErrorTrueResponseStatusZero() {
        // given
        FeignException feignException = mock(FeignException.class);
        when(feignException.contentUTF8()).thenReturn(getFeignResponseAsString("200", true, "0"));
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenThrow(feignException);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            createExportBasedOnSearchResultsTableCheckService.createExportBasedOnSearchResultsTable(
                BEARER_TOKEN, 1, headerColumns, PRODUCTION_NAME, pollDuration, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: ARM RPO API failed with status - 200 OK and response"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WithStatus400IsErrorTrueResponseStatusZero() {
        // given
        CreateExportBasedOnSearchResultsTableResponse response = createResponse(400, true, 0);
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenReturn(response);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            createExportBasedOnSearchResultsTableCheckService.createExportBasedOnSearchResultsTable(
                BEARER_TOKEN, 1, headerColumns, PRODUCTION_NAME, pollDuration, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: ARM RPO API failed with invalid status - 400 BAD_REQUEST"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WhenFeignExceptionThrownWithStatus400IsErrorTrueResponseStatusZero() {
        // given
        FeignException feignException = mock(FeignException.class);
        when(feignException.contentUTF8()).thenReturn(getFeignResponseAsString("200", true, "0"));
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenThrow(feignException);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            createExportBasedOnSearchResultsTableCheckService.createExportBasedOnSearchResultsTable(BEARER_TOKEN, 1, headerColumns, PRODUCTION_NAME,
                                                                                                    pollDuration, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: ARM RPO API failed with status - 200 OK and response"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WithStatus400IsErrorFalseResponseStatusZero() {
        // given
        CreateExportBasedOnSearchResultsTableResponse response = createResponse(400, false, 0);
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenReturn(response);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            createExportBasedOnSearchResultsTableCheckService.createExportBasedOnSearchResultsTable(BEARER_TOKEN, 1, headerColumns, PRODUCTION_NAME,
                                                                                                    pollDuration, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: ARM RPO API failed with invalid status - 400 BAD_REQUEST"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WhenFeignExceptionThrownWithStatus400IsErrorFalseResponseStatusZero() {
        // given
        FeignException feignException = mock(FeignException.class);
        when(feignException.contentUTF8()).thenReturn(getFeignResponseAsString("200", true, "0"));
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenThrow(feignException);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            createExportBasedOnSearchResultsTableCheckService.createExportBasedOnSearchResultsTable(BEARER_TOKEN, 1, headerColumns, PRODUCTION_NAME,
                                                                                                    pollDuration, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: ARM RPO API failed with status - 200 OK and response"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WithStatus500IsErrorFalseResponseStatus500() {
        // given
        CreateExportBasedOnSearchResultsTableResponse response = createResponse(500, false, 500);
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenReturn(response);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            createExportBasedOnSearchResultsTableCheckService.createExportBasedOnSearchResultsTable(BEARER_TOKEN, 1, headerColumns, PRODUCTION_NAME,
                                                                                                    pollDuration, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: ARM RPO API failed with status - 500 INTERNAL_SERVER_ERROR"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WhenFeignExceptionThrownWithStatus500IsErrorFalseResponseStatus0() {
        // given
        FeignException feignException = mock(FeignException.class);
        when(feignException.contentUTF8()).thenReturn(getFeignResponseAsString("500", true, "0"));
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenThrow(feignException);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            createExportBasedOnSearchResultsTableCheckService.createExportBasedOnSearchResultsTable(BEARER_TOKEN, 1, headerColumns, PRODUCTION_NAME,
                                                                                                    pollDuration, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: ARM RPO API failed with status - 500 INTERNAL_SERVER_ERROR and response"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WithInvalidRequest() {
        // given
        armRpoExecutionDetailEntity.setSearchId(null);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            createExportBasedOnSearchResultsTableCheckService.createExportBasedOnSearchResultsTable(
                BEARER_TOKEN, 1, headerColumns, PRODUCTION_NAME, pollDuration, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: Could not construct API request"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WhenClientThrowsFeignException() {
        // given
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenThrow(FeignException.class);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            createExportBasedOnSearchResultsTableCheckService.createExportBasedOnSearchResultsTable(
                BEARER_TOKEN, 1, headerColumns, PRODUCTION_NAME, pollDuration, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: Unable to get ARM RPO response"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WhenFeignExceptionThrownWithStatus500IsErrorFalseResponseStatus500() {
        // given
        FeignException feignException = mock(FeignException.class);
        when(feignException.contentUTF8()).thenReturn(getFeignResponseAsString("500", true, "500"));
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenThrow(feignException);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            createExportBasedOnSearchResultsTableCheckService.createExportBasedOnSearchResultsTable(BEARER_TOKEN, 1, headerColumns, PRODUCTION_NAME,
                                                                                                    pollDuration, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: ARM RPO API failed with status - 500 INTERNAL_SERVER_ERROR"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WhenFeignExceptionThrownWithNullResponse() {
        // given
        FeignException feignException = mock(FeignException.class);
        when(feignException.contentUTF8()).thenReturn(null);
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenThrow(feignException);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            createExportBasedOnSearchResultsTableCheckService.createExportBasedOnSearchResultsTable(BEARER_TOKEN, 1, headerColumns, PRODUCTION_NAME,
                                                                                                    pollDuration, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: Unable to get ARM RPO response from client"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WhenFeignExceptionThrownWithEmptyResponse() {
        // given
        FeignException feignException = mock(FeignException.class);
        when(feignException.contentUTF8()).thenReturn("");
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenThrow(feignException);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            createExportBasedOnSearchResultsTableCheckService.createExportBasedOnSearchResultsTable(BEARER_TOKEN, 1, headerColumns, PRODUCTION_NAME,
                                                                                                    pollDuration, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: Unable to get ARM RPO response from client"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WhenFeignExceptionThrownWithInvalidJsonResponse() {
        // given
        FeignException feignException = mock(FeignException.class);
        when(feignException.contentUTF8()).thenReturn("{");
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenThrow(feignException);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            createExportBasedOnSearchResultsTableCheckService.createExportBasedOnSearchResultsTable(
                BEARER_TOKEN, 1, headerColumns, PRODUCTION_NAME, pollDuration, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: Unable to get ARM RPO response from client"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ThrowsException_WhenFeignExceptionThrownWithEmptyResponseObject() {
        // given
        FeignException feignException = mock(FeignException.class);
        when(feignException.contentUTF8()).thenReturn("{}");
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenThrow(feignException);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            createExportBasedOnSearchResultsTableCheckService.createExportBasedOnSearchResultsTable(
                BEARER_TOKEN, 1, headerColumns, PRODUCTION_NAME, pollDuration, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: ARM RPO API createExportBasedOnSearchResultsTable is invalid"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void createExportBasedOnSearchResultsTable_ShouldRetryOnUnauthorised_WhenResponseIsValid() {
        // given
        Response response = Response.builder()
            .request(Request.create(Request.HttpMethod.POST, "/createExportBasedOnSearchResultsTable", java.util.Map.of(), null,
                                    StandardCharsets.UTF_8, null))
            .status(401)
            .reason("Unauthorised")
            .build();
        FeignException feign401 = FeignException.errorStatus("createExportBasedOnSearchResultsTable", response);
        when(armRpoClient.createExportBasedOnSearchResultsTable(eq("token"), any())).thenThrow(feign401);

        // armRpoUtil should be asked for a new token
        doReturn("Bearer refreshed").when(armRpoUtil).retryGetBearerToken(anyString());

        var createExportBasedOnSearchResultsTableResponse = createResponse(200, false, 0);
        when(armRpoClient.createExportBasedOnSearchResultsTable(eq("Bearer refreshed"), any())).thenReturn(createExportBasedOnSearchResultsTableResponse);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();

        // when
        boolean result = createExportBasedOnSearchResultsTableCheckService.createExportBasedOnSearchResultsTable(
            BEARER_TOKEN, 1, headerColumns, PRODUCTION_NAME, pollDuration, userAccount);

        // then
        assertTrue(result);
        // assert that the productionName of armRpoExecutionDetailEntity contains the production name
        assertThat(armRpoExecutionDetailEntity.getProductionName(), containsString(PRODUCTION_NAME));
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getCompletedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void createExportBasedOnSearchResultsTable_ShouldRetryOnForbidden_WhenResponseIsValid() {
        // given
        Response response = Response.builder()
            .request(Request.create(Request.HttpMethod.POST, "/createExportBasedOnSearchResultsTable", java.util.Map.of(), null,
                                    StandardCharsets.UTF_8, null))
            .status(403)
            .reason("Forbidden")
            .build();
        FeignException feign403 = FeignException.errorStatus("createExportBasedOnSearchResultsTable", response);
        when(armRpoClient.createExportBasedOnSearchResultsTable(eq("token"), any())).thenThrow(feign403);

        // armRpoUtil should be asked for a new token
        doReturn("Bearer refreshed").when(armRpoUtil).retryGetBearerToken(anyString());

        var createExportBasedOnSearchResultsTableResponse = createResponse(200, false, 0);
        when(armRpoClient.createExportBasedOnSearchResultsTable(eq("Bearer refreshed"), any())).thenReturn(createExportBasedOnSearchResultsTableResponse);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();

        // when
        boolean result = createExportBasedOnSearchResultsTableCheckService.createExportBasedOnSearchResultsTable(
            BEARER_TOKEN, 1, headerColumns, PRODUCTION_NAME, pollDuration, userAccount);

        // then
        assertTrue(result);
        // assert that the productionName of armRpoExecutionDetailEntity contains the production name
        assertThat(armRpoExecutionDetailEntity.getProductionName(), containsString(PRODUCTION_NAME));
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getCompletedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void createExportBasedOnSearchResultsTable_ShouldRetryOnUnauthorised_ThenFailSecondUnauthorised() {
        // given
        Response response = Response.builder()
            .request(Request.create(Request.HttpMethod.POST, "/createExportBasedOnSearchResultsTable", java.util.Map.of(), null,
                                    StandardCharsets.UTF_8, null))
            .status(401)
            .reason("Unauthorised")
            .build();
        FeignException feign401 = FeignException.errorStatus("createExportBasedOnSearchResultsTable", response);
        when(armRpoClient.createExportBasedOnSearchResultsTable(eq("token"), any())).thenThrow(feign401);

        // armRpoUtil should be asked for a new token
        doReturn("Bearer refreshed").when(armRpoUtil).retryGetBearerToken(anyString());

        when(armRpoClient.createExportBasedOnSearchResultsTable(eq("Bearer refreshed"), any())).thenThrow(feign401);
        List<MasterIndexFieldByRecordClassSchema> headerColumns = createHeaderColumns();

        // when
        ArmRpoException exception = assertThrows(ArmRpoException.class, () ->
            createExportBasedOnSearchResultsTableCheckService.createExportBasedOnSearchResultsTable(
                BEARER_TOKEN, 1, headerColumns, PRODUCTION_NAME, pollDuration, userAccount));

        // then
        assertThat(exception.getMessage(), containsString("Failure during ARM createExportBasedOnSearchResultsTable"));
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(armRpoHelperMocks.getCreateExportBasedOnSearchResultsTableRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelperMocks.getFailedRpoStatus()), any());
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

    private CreateExportBasedOnSearchResultsTableResponse createResponse(int status, boolean isError, int responseStatus) {
        CreateExportBasedOnSearchResultsTableResponse response = new CreateExportBasedOnSearchResultsTableResponse();
        response.setStatus(status);
        response.setIsError(isError);
        response.setResponseStatus(responseStatus);
        return response;
    }

    @AfterEach
    void close() {
        armRpoHelperMocks.close();
    }
}