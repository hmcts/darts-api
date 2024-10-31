package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.MasterIndexFieldByRecordClassSchemaResponse;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.model.rpo.MasterIndexFieldByRecordClassSchema;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStateEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArmRpoApiGetMasterIndexFieldByRecordClassSchemaTest {

    @Mock
    private ArmRpoClient armRpoClient;

    @Mock
    private ArmRpoService armRpoService;

    @InjectMocks
    private ArmRpoApiImpl armRpoApi;

    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity;
    private UserAccountEntity userAccount;
    private ArmRpoStateEntity rpoStateEntity;

    @BeforeEach
    void setUp() {
        armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        userAccount = new UserAccountEntity();
        rpoStateEntity = new ArmRpoStateEntity();
    }

    @Test
    void getMasterIndexFieldByRecordClassSchema_success() {
        MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField masterIndexField = new MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField();
        masterIndexField.setMasterIndexFieldId("1");
        masterIndexField.setDisplayName("displayName");
        masterIndexField.setPropertyName("propertyName");
        masterIndexField.setPropertyType("propertyType");

        MasterIndexFieldByRecordClassSchemaResponse response = new MasterIndexFieldByRecordClassSchemaResponse();
        response.setMasterIndexFields();

        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.getMasterIndexFieldByRecordClassSchema(anyString(), any())).thenReturn(response);

        List<MasterIndexFieldByRecordClassSchema> result = armRpoApi.getMasterIndexFieldByRecordClassSchema("token", 1, rpoStateEntity, userAccount);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(armRpoService).updateArmRpoStateAndStatus(any(), any(), any(), any());
        verify(armRpoService).updateArmRpoStatus(any(), any(), any());
    }

    @Test
    void getMasterIndexFieldByRecordClassSchemaWhereClientThrowsFeignException() {
        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.getMasterIndexFieldByRecordClassSchema(anyString(), any())).thenThrow(FeignException.class);

        assertThrows(ArmRpoException.class, () -> armRpoApi.getMasterIndexFieldByRecordClassSchema("token", 1, rpoStateEntity, userAccount));
        verify(armRpoService).updateArmRpoStateAndStatus(any(), any(), any(), any());
        verify(armRpoService).updateArmRpoStatus(any(), any(), any());
    }

    @Test
    void getMasterIndexFieldByRecordClassSchemaWithNullResponse() {
        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.getMasterIndexFieldByRecordClassSchema(anyString(), any())).thenReturn(null);

        assertThrows(ArmRpoException.class, () -> armRpoApi.getMasterIndexFieldByRecordClassSchema("token", 1, rpoStateEntity, userAccount));
        verify(armRpoService).updateArmRpoStateAndStatus(any(), any(), any(), any());
        verify(armRpoService).updateArmRpoStatus(any(), any(), any());
    }

    @Test
    void getMasterIndexFieldByRecordClassSchemaWithEmptyResponse() {
        MasterIndexFieldByRecordClassSchemaResponse response = new MasterIndexFieldByRecordClassSchemaResponse();
        response.setMasterIndexFields(Collections.emptyList());

        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.getMasterIndexFieldByRecordClassSchema(anyString(), any())).thenReturn(response);

        assertThrows(ArmRpoException.class, () -> armRpoApi.getMasterIndexFieldByRecordClassSchema("token", 1, rpoStateEntity, userAccount));
        verify(armRpoService).updateArmRpoStateAndStatus(any(), any(), any(), any());
        verify(armRpoService).updateArmRpoStatus(any(), any(), any());
    }
}