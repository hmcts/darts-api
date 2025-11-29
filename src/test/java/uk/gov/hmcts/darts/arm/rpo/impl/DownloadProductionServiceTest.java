package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.Response.Body;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.darts.arm.component.ArmRpoDownloadProduction;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelperMocks;
import uk.gov.hmcts.darts.arm.service.ArmApiService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@TestPropertySource(properties = {"darts.storage.arm.is-mock-arm-rpo-download-csv=true"})
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.CloseResource")
class DownloadProductionServiceTest {

    private static final Integer EXECUTION_ID = 1;
    private static final String BEARER_TOKEN = "token";

    @Mock
    private ArmRpoService armRpoService;
    @Mock
    private ArmApiService armApiService;

    @Mock
    private ArmRpoDownloadProduction armRpoDownloadProduction;

    private DownloadProductionServiceImpl downloadProductionService;

    @Captor
    private ArgumentCaptor<ArmRpoExecutionDetailEntity> armRpoExecutionDetailEntityArgumentCaptor;

    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity;
    private UserAccountEntity userAccount;
    private ArmRpoUtil armRpoUtil;

    private ArmRpoHelperMocks armRpoHelperMocks;

    @BeforeEach
    void setUp() {
        armRpoHelperMocks = new ArmRpoHelperMocks();
        armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(EXECUTION_ID);
        userAccount = new UserAccountEntity();
        armRpoExecutionDetailEntityArgumentCaptor = ArgumentCaptor.forClass(ArmRpoExecutionDetailEntity.class);
        armRpoUtil = spy(new ArmRpoUtil(armRpoService, armApiService));
        downloadProductionService = new DownloadProductionServiceImpl(armRpoService, armRpoUtil, armRpoDownloadProduction);
    }

    @Test
    void downloadProduction_Success() throws IOException {
        // given
        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        Response response = mock(Response.class);
        when(response.status()).thenReturn(200);
        InputStream inputStream = mock(InputStream.class);
        Body body = mock(Body.class);
        when(response.body()).thenReturn(body);
        when(body.asInputStream()).thenReturn(inputStream);
        when(armRpoDownloadProduction.downloadProduction(anyString(), anyInt(), anyString())).thenReturn(response);

        try (InputStream result =
                 downloadProductionService.downloadProduction(BEARER_TOKEN, EXECUTION_ID, "productionExportId", userAccount)) {
            // then
            assertNotNull(result);
        }

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntityArgumentCaptor.capture(),
                                                         eq(armRpoHelperMocks.getDownloadProductionRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity, armRpoHelperMocks.getCompletedRpoStatus(), userAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void downloadProduction_Returns400Error() {
        // given
        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        Response response = mock(Response.class);
        when(response.status()).thenReturn(400);
        when(armRpoDownloadProduction.downloadProduction(anyString(), anyInt(), anyString())).thenReturn(response);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            downloadProductionService.downloadProduction(BEARER_TOKEN, EXECUTION_ID, "productionExportId", userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during download production: Failed ARM RPO download production with id: productionExportId"));

        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntityArgumentCaptor.capture(),
                                                         eq(armRpoHelperMocks.getDownloadProductionRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(eq(armRpoExecutionDetailEntity), eq(armRpoHelperMocks.getFailedRpoStatus()), eq(userAccount));
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void downloadProduction_ThrowsFeignException() {
        // given
        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoDownloadProduction.downloadProduction(anyString(), anyInt(), anyString())).thenThrow(FeignException.class);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            downloadProductionService.downloadProduction(BEARER_TOKEN, EXECUTION_ID, "productionExportId", userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during download production: Error during ARM RPO download production id: productionExportId"));

        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntityArgumentCaptor.capture(),
                                                         eq(armRpoHelperMocks.getDownloadProductionRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(eq(armRpoExecutionDetailEntity), eq(armRpoHelperMocks.getFailedRpoStatus()), eq(userAccount));
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void downloadProduction_ShouldRetryOnUnauthorised_WhenResponseIsValid() throws IOException {
        // given
        Response response = Response.builder()
            .request(Request.create(Request.HttpMethod.POST, "/downloadProduction", java.util.Map.of(), null,
                                    StandardCharsets.UTF_8, null))
            .status(401)
            .reason("Unauthorised")
            .build();
        FeignException feign401 = FeignException.errorStatus("downloadProduction", response);
        when(armRpoDownloadProduction.downloadProduction(eq(BEARER_TOKEN), anyInt(), anyString())).thenThrow(feign401);

        // armRpoUtil should be asked for a new token
        doReturn("Bearer refreshed").when(armRpoUtil).retryGetBearerToken(anyString());

        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        Response responseSuccess = mock(Response.class);
        when(responseSuccess.status()).thenReturn(200);
        InputStream inputStream = mock(InputStream.class);
        Body body = mock(Body.class);
        when(responseSuccess.body()).thenReturn(body);
        when(body.asInputStream()).thenReturn(inputStream);
        when(armRpoDownloadProduction.downloadProduction(eq("Bearer refreshed"), anyInt(), anyString())).thenReturn(responseSuccess);

        try (InputStream result =
                 downloadProductionService.downloadProduction(BEARER_TOKEN, EXECUTION_ID, "productionExportId", userAccount)) {
            // then
            assertNotNull(result);
        }

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntityArgumentCaptor.capture(),
                                                         eq(armRpoHelperMocks.getDownloadProductionRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity, armRpoHelperMocks.getCompletedRpoStatus(), userAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void downloadProduction_ShouldRetryOnForbidden_WhenResponseIsValid() throws IOException {
        // given
        Response response = Response.builder()
            .request(Request.create(Request.HttpMethod.POST, "/downloadProduction", java.util.Map.of(), null,
                                    StandardCharsets.UTF_8, null))
            .status(403)
            .reason("Forbidden")
            .build();
        FeignException feign403 = FeignException.errorStatus("downloadProduction", response);
        when(armRpoDownloadProduction.downloadProduction(eq(BEARER_TOKEN), anyInt(), anyString())).thenThrow(feign403);

        // armRpoUtil should be asked for a new token
        doReturn("Bearer refreshed").when(armRpoUtil).retryGetBearerToken(anyString());

        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        Response responseSuccess = mock(Response.class);
        when(responseSuccess.status()).thenReturn(200);
        InputStream inputStream = mock(InputStream.class);
        Body body = mock(Body.class);
        when(responseSuccess.body()).thenReturn(body);
        when(body.asInputStream()).thenReturn(inputStream);
        when(armRpoDownloadProduction.downloadProduction(eq("Bearer refreshed"), anyInt(), anyString())).thenReturn(responseSuccess);

        try (InputStream result =
                 downloadProductionService.downloadProduction(BEARER_TOKEN, EXECUTION_ID, "productionExportId", userAccount)) {
            // then
            assertNotNull(result);
        }

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntityArgumentCaptor.capture(),
                                                         eq(armRpoHelperMocks.getDownloadProductionRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity, armRpoHelperMocks.getCompletedRpoStatus(), userAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void downloadProduction_ShouldRetryOnUnauthorised_ThenFailSecondUnauthorised() {
        // given
        Response response = Response.builder()
            .request(Request.create(Request.HttpMethod.POST, "/downloadProduction", java.util.Map.of(), null,
                                    StandardCharsets.UTF_8, null))
            .status(401)
            .reason("Unauthorised")
            .build();
        FeignException feign401 = FeignException.errorStatus("downloadProduction", response);
        when(armRpoDownloadProduction.downloadProduction(eq(BEARER_TOKEN), anyInt(), anyString())).thenThrow(feign401);

        // armRpoUtil should be asked for a new token
        doReturn("Bearer refreshed").when(armRpoUtil).retryGetBearerToken(anyString());

        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoDownloadProduction.downloadProduction(eq("Bearer refreshed"), anyInt(), anyString())).thenThrow(feign401);

        ArmRpoException exception = assertThrows(ArmRpoException.class, () ->
            downloadProductionService.downloadProduction(BEARER_TOKEN, EXECUTION_ID, "productionExportId", userAccount));

        // then
        assertThat(exception.getMessage(), containsString("Failure during download production: Error during ARM RPO download production id"));
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntityArgumentCaptor.capture(),
                                                         eq(armRpoHelperMocks.getDownloadProductionRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity, armRpoHelperMocks.getFailedRpoStatus(), userAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @AfterEach
    void close() {
        armRpoHelperMocks.close();
    }

}