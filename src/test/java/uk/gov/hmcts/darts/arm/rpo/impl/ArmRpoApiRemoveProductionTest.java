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
import uk.gov.hmcts.darts.arm.client.model.rpo.RemoveProductionRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.RemoveProductionResponse;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelperMocks;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArmRpoApiRemoveProductionTest {

    private static final Integer EXECUTION_ID = 1;
    private static final String BEARER_TOKEN = "token";

    @Mock
    private ArmRpoClient armRpoClient;

    @Mock
    private ArmRpoService armRpoService;

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
    void removeProductionSuccess() {
        // given
        RemoveProductionResponse response = new RemoveProductionResponse();
        response.setStatus(200);
        response.setIsError(false);

        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.removeProduction(anyString(), any(RemoveProductionRequest.class))).thenReturn(response);

        // when
        armRpoApi.removeProduction(BEARER_TOKEN, EXECUTION_ID, userAccount);

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntityArgumentCaptor.capture(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getRemoveProductionRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(eq(armRpoExecutionDetailEntity), eq(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus()), eq(userAccount));
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void removeProductionThrowsFeignException() {
        // given
        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.removeProduction(anyString(), any(RemoveProductionRequest.class))).thenThrow(FeignException.class);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () -> armRpoApi.removeProduction("token", 1, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM RPO removeProduction: Unable to get ARM RPO response"));
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntityArgumentCaptor.capture(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getRemoveProductionRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(eq(armRpoExecutionDetailEntity), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), eq(userAccount));
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void removeProductionWithNullResponse() {
        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.removeProduction(anyString(), any(RemoveProductionRequest.class))).thenReturn(null);

        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () -> armRpoApi.removeProduction("token", 1, userAccount));

        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM RPO removeProduction: ARM RPO API response is invalid"));
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntityArgumentCaptor.capture(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getRemoveProductionRpoState()),
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