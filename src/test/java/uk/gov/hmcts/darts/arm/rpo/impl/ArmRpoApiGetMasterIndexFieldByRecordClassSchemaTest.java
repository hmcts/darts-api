package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.MasterIndexFieldByRecordClassSchemaResponse;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelperMocks;
import uk.gov.hmcts.darts.arm.model.rpo.MasterIndexFieldByRecordClassSchema;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    private static final ArmRpoHelperMocks ARM_RPO_HELPER_MOCKS = new ArmRpoHelperMocks();

    @BeforeEach
    void setUp() {
        armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        userAccount = new UserAccountEntity();
    }

    @Test
    void getMasterIndexFieldByRecordClassSchemaSuccess() {
        // given
        MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField masterIndexField1 = new MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField();
        masterIndexField1.setMasterIndexFieldId("1");
        masterIndexField1.setDisplayName("displayName");
        masterIndexField1.setPropertyName("propertyName");
        masterIndexField1.setPropertyType("propertyType");
        masterIndexField1.setIsMasked(true);
        MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField masterIndexField2 = new MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField();
        masterIndexField2.setMasterIndexFieldId("2");
        masterIndexField2.setDisplayName("displayName");
        masterIndexField2.setPropertyName("ingestionDate");
        masterIndexField2.setPropertyType("propertyType");
        masterIndexField2.setIsMasked(false);

        MasterIndexFieldByRecordClassSchemaResponse response = new MasterIndexFieldByRecordClassSchemaResponse();
        response.setMasterIndexFields(List.of(masterIndexField1, masterIndexField2));

        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.getMasterIndexFieldByRecordClassSchema(anyString(), any())).thenReturn(response);

        // when
        List<MasterIndexFieldByRecordClassSchema> result = armRpoApi.getMasterIndexFieldByRecordClassSchema(
            "token", 1, ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaPrimaryRpoState(), userAccount);

        // then
        assertNotNull(result);
        MasterIndexFieldByRecordClassSchema masterIndexFieldByRecordClassSchema = result.getFirst();
        assertEquals("1", masterIndexFieldByRecordClassSchema.getMasterIndexField());
        assertEquals("displayName", masterIndexFieldByRecordClassSchema.getDisplayName());
        assertEquals("propertyName", masterIndexFieldByRecordClassSchema.getPropertyName());
        assertEquals("propertyType", masterIndexFieldByRecordClassSchema.getPropertyType());
        assertTrue(masterIndexFieldByRecordClassSchema.getIsMasked());

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetMasterIndexFieldByRecordClassSchemaPrimaryRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus()), any());

    }

    @Test
    void getMasterIndexFieldByRecordClassSchemaWhereClientThrowsFeignException() {
        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.getMasterIndexFieldByRecordClassSchema(anyString(), any())).thenThrow(FeignException.class);

        assertThrows(ArmRpoException.class, () -> armRpoApi.getMasterIndexFieldByRecordClassSchema(
            "token", 1, ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaPrimaryRpoState(), userAccount));
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetMasterIndexFieldByRecordClassSchemaPrimaryRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
    }

    @Test
    void getMasterIndexFieldByRecordClassSchemaWithNullResponse() {
        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.getMasterIndexFieldByRecordClassSchema(anyString(), any())).thenReturn(null);

        assertThrows(ArmRpoException.class, () -> armRpoApi.getMasterIndexFieldByRecordClassSchema(
            "token", 1, ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaPrimaryRpoState(), userAccount));
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetMasterIndexFieldByRecordClassSchemaPrimaryRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
    }

    @Test
    void getMasterIndexFieldByRecordClassSchemaWithEmptyResponse() {
        MasterIndexFieldByRecordClassSchemaResponse response = new MasterIndexFieldByRecordClassSchemaResponse();
        response.setMasterIndexFields(Collections.emptyList());

        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.getMasterIndexFieldByRecordClassSchema(anyString(), any())).thenReturn(response);

        assertThrows(ArmRpoException.class, () -> armRpoApi.getMasterIndexFieldByRecordClassSchema(
            "token", 1, ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaPrimaryRpoState(), userAccount));
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetMasterIndexFieldByRecordClassSchemaPrimaryRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
    }

    @AfterAll
    static void close() {
        ARM_RPO_HELPER_MOCKS.close();
    }
}