package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.RecordManagementMatterResponse;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelperMocks;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetRecordManagementMatterServiceTest {

    @Mock
    private ArmRpoClient armRpoClient;

    @Mock
    private ArmRpoService armRpoService;

    private GetRecordManagementMatterServiceImpl getRecordManagementMatterService;

    private UserAccountEntity userAccountEntity;

    private static final Integer EXECUTION_ID = 1;
    private static final ArmRpoHelperMocks ARM_RPO_HELPER_MOCKS = new ArmRpoHelperMocks();


    @BeforeEach
    void setUp() {
        userAccountEntity = new UserAccountEntity();

        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(EXECUTION_ID);
        when(armRpoService.getArmRpoExecutionDetailEntity(EXECUTION_ID)).thenReturn(armRpoExecutionDetailEntity);
        ArmRpoUtil armRpoUtil = new ArmRpoUtil(armRpoService);
        getRecordManagementMatterService = new GetRecordManagementMatterServiceImpl(armRpoClient, armRpoService, armRpoUtil);
    }

    @Test
    void getRecordManagementMatterThrowsExceptionWhenResponseReturnedWithoutMatterId() {
        // given
        RecordManagementMatterResponse expectedResponse = new RecordManagementMatterResponse();
        expectedResponse.setStatus(200);
        expectedResponse.setIsError(false);
        when(armRpoClient.getRecordManagementMatter(anyString(), any())).thenReturn(expectedResponse);

        // when
        assertThrows(ArmRpoException.class, () -> getRecordManagementMatterService.getRecordManagementMatter("token", EXECUTION_ID, userAccountEntity));

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetRecordManagementMatterRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
    }

    @Test
    void getRecordManagementMatterThrowsArmRpoExceptionWhenClientFails() {
        // given
        when(armRpoClient.getRecordManagementMatter(anyString(), any())).thenThrow(FeignException.class);

        // when
        assertThrows(ArmRpoException.class, () -> getRecordManagementMatterService.getRecordManagementMatter("token", EXECUTION_ID, userAccountEntity));

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetRecordManagementMatterRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
    }

    @Test
    void getRecordManagementMatterUpdatesStatusToInProgress() {
        // when
        assertThrows(ArmRpoException.class, () -> getRecordManagementMatterService.getRecordManagementMatter("token", EXECUTION_ID, userAccountEntity));

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetRecordManagementMatterRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
    }

    @Test
    void getRecordManagementMatterSetsMatterIdWhenResponseIsValid() {
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
        getRecordManagementMatterService.getRecordManagementMatter("token", executionId, userAccountEntity);

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetRecordManagementMatterRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus()), any());
    }

    @AfterAll
    static void close() {
        ARM_RPO_HELPER_MOCKS.close();
    }
}