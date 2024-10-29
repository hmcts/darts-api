package uk.gov.hmcts.darts.arm.rpo.impl;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.model.rpo.RecordManagementMatterResponse;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArmRpoApiGetRecordManagementMatterTest {

    @Mock
    private ArmRpoClient armRpoClient;

    @Mock
    private ArmRpoService armRpoService;

    @Mock
    private ArmRpoHelper armRpoHelper;

    @InjectMocks
    private ArmRpoApiImpl armRpoApi;

    private UserAccountEntity userAccountEntity;

    private final Integer executionId = 1;

    @BeforeEach
    void setUp() {
        userAccountEntity = new UserAccountEntity();

        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(executionId);
        when(armRpoService.getArmRpoExecutionDetailEntity(executionId)).thenReturn(armRpoExecutionDetailEntity);

    }

    @Test
    void getRecordManagementMatterReturnsResponseWhenSuccessful() {
        RecordManagementMatterResponse expectedResponse = new RecordManagementMatterResponse();
        when(armRpoClient.getRecordManagementMatter(anyString())).thenReturn(expectedResponse);

        RecordManagementMatterResponse response = armRpoApi.getRecordManagementMatter("token", executionId, userAccountEntity);

        assertEquals(expectedResponse, response);
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelper.completedRpoStatus()), eq(userAccountEntity));
    }

    @Test
    void getRecordManagementMatterThrowsFeignExceptionWhenClientFails() {
        when(armRpoClient.getRecordManagementMatter(anyString())).thenThrow(FeignException.class);

        assertThrows(FeignException.class, () -> armRpoApi.getRecordManagementMatter("token", executionId, userAccountEntity));
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelper.failedRpoStatus()), eq(userAccountEntity));
    }

    @Test
    void getRecordManagementMatterUpdatesStatusToInProgress() {
        // when
        armRpoApi.getRecordManagementMatter("token", executionId, userAccountEntity);

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(any(), eq(armRpoHelper.getRecordManagementMatterRpoState()), eq(armRpoHelper.inProgressRpoStatus()),
                                                         eq(userAccountEntity));
    }

    @Test
    void getRecordManagementMatterSetsMatterIdWhenResponseIsValid() {
        // given
        RecordManagementMatterResponse response = new RecordManagementMatterResponse();
        response.setRecordManagementMatter(new RecordManagementMatterResponse.RecordManagementMatter());
        response.getRecordManagementMatter().setMatterId("123");
        when(armRpoClient.getRecordManagementMatter(anyString())).thenReturn(response);

        Integer executionId = 1;
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(executionId);
        when(armRpoService.getArmRpoExecutionDetailEntity(executionId)).thenReturn(armRpoExecutionDetailEntity);

        // when
        armRpoApi.getRecordManagementMatter("token", executionId, userAccountEntity);

        // then
        verify(armRpoService).updateArmRpoStatus(any(), eq(armRpoHelper.completedRpoStatus()), eq(userAccountEntity));
    }

}