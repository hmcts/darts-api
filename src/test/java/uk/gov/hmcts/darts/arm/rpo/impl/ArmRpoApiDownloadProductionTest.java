package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelperMocks;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
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

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.CloseResource")
class ArmRpoApiDownloadProductionTest {

    private static final Integer EXECUTION_ID = 1;
    private static final String BEARER_TOKEN = "token";

    @Mock
    private ArmRpoClient armRpoClient;

    @Mock
    private ArmRpoService armRpoService;

    @Mock
    private DownloadResponseMetaData metaData;


    @InjectMocks
    private ArmRpoApiImpl armRpoApi;

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
        when(armRpoClient.downloadProduction(anyString(), anyString())).thenReturn(response);

        when(armRpoClient.downloadProduction(anyString(), anyString())).thenReturn(response);

        try (InputStream result =
                 armRpoApi.downloadProduction(BEARER_TOKEN, EXECUTION_ID, "productionExportId", userAccount)) {
            // then
            assertNotNull(result);
        }

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntityArgumentCaptor.capture(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getDownloadProductionRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(eq(armRpoExecutionDetailEntity), eq(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus()), eq(userAccount));
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void downloadProductionReturns400Error() {
        // given
        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        feign.Response response = mock(feign.Response.class);
        when(response.status()).thenReturn(400);
        when(armRpoClient.downloadProduction(anyString(), anyString())).thenReturn(response);

        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            armRpoApi.downloadProduction(BEARER_TOKEN, EXECUTION_ID, "productionExportId", userAccount));

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
        when(armRpoClient.downloadProduction(anyString(), anyString())).thenThrow(FeignException.class);


        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            armRpoApi.downloadProduction(BEARER_TOKEN, EXECUTION_ID, "productionExportId", userAccount));

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