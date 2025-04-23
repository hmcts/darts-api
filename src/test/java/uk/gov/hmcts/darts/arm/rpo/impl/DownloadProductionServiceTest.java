package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import org.junit.jupiter.api.AfterAll;
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
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
    private ArmRpoDownloadProduction armRpoDownloadProduction;

    private DownloadProductionServiceImpl downloadProductionService;

    @Captor
    private ArgumentCaptor<ArmRpoExecutionDetailEntity> armRpoExecutionDetailEntityArgumentCaptor;

    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity;
    private UserAccountEntity userAccount;

    private static final ArmRpoHelperMocks ARM_RPO_HELPER_MOCKS = new ArmRpoHelperMocks();


    @BeforeEach
    void setUp() {
        armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(EXECUTION_ID);
        userAccount = new UserAccountEntity();
        armRpoExecutionDetailEntityArgumentCaptor = ArgumentCaptor.forClass(ArmRpoExecutionDetailEntity.class);
        ArmRpoUtil armRpoUtil = new ArmRpoUtil(armRpoService);
        downloadProductionService = new DownloadProductionServiceImpl(armRpoService, armRpoUtil, armRpoDownloadProduction);
    }

    @Test
    void downloadProductionSuccess() throws IOException {
        // given
        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        feign.Response response = mock(feign.Response.class);
        when(response.status()).thenReturn(200);
        InputStream inputStream = mock(InputStream.class);
        feign.Response.Body body = mock(feign.Response.Body.class);
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
                                                         eq(ARM_RPO_HELPER_MOCKS.getDownloadProductionRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity, ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus(), userAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void downloadProductionReturns400Error() {
        // given
        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        feign.Response response = mock(feign.Response.class);
        when(response.status()).thenReturn(400);
        when(armRpoDownloadProduction.downloadProduction(anyString(), anyInt(), anyString())).thenReturn(response);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            downloadProductionService.downloadProduction(BEARER_TOKEN, EXECUTION_ID, "productionExportId", userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during download production: Failed ARM RPO download production with id: productionExportId"));

        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntityArgumentCaptor.capture(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getDownloadProductionRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(eq(armRpoExecutionDetailEntity), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), eq(userAccount));
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void downloadProductionThrowsFeignException() {
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
                                                         eq(ARM_RPO_HELPER_MOCKS.getDownloadProductionRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(eq(armRpoExecutionDetailEntity), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), eq(userAccount));
        verifyNoMoreInteractions(armRpoService);
    }

    @AfterAll
    static void close() {
        ARM_RPO_HELPER_MOCKS.close();
    }

}