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
import uk.gov.hmcts.darts.arm.client.model.rpo.MasterIndexFieldByRecordClassSchemaResponse;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelperMocks;
import uk.gov.hmcts.darts.arm.model.rpo.MasterIndexFieldByRecordClassSchema;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.ArmRpoExecutionDetailRepository;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArmRpoApiGetMasterIndexFieldByRecordClassSchemaTest {

    private static final Integer EXECUTION_ID = 1;
    private static final String BEARER_TOKEN = "token";

    @Mock
    private ArmRpoClient armRpoClient;

    @Mock
    private ArmRpoService armRpoService;

    @Mock
    private ArmRpoExecutionDetailRepository armRpoExecutionDetailRepository;

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

        MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField masterIndexField3 = new MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField();
        masterIndexField3.setMasterIndexFieldId("3");
        masterIndexField3.setDisplayName("displayName");
        masterIndexField3.setPropertyName("bf_018");
        masterIndexField3.setPropertyType("propertyType");
        masterIndexField3.setIsMasked(false);

        MasterIndexFieldByRecordClassSchemaResponse response = new MasterIndexFieldByRecordClassSchemaResponse();
        response.setMasterIndexFields(List.of(masterIndexField1, masterIndexField2, masterIndexField3));

        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.getMasterIndexFieldByRecordClassSchema(anyString(), any())).thenReturn(response);

        // when
        List<MasterIndexFieldByRecordClassSchema> result = armRpoApi.getMasterIndexFieldByRecordClassSchema(
            BEARER_TOKEN, EXECUTION_ID, ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaPrimaryRpoState(), userAccount);

        // then
        assertNotNull(result);
        assertEquals(2, result.size());

        MasterIndexFieldByRecordClassSchema masterIndexFieldByRecordClassSchema = result.getFirst();
        assertEquals("1", masterIndexFieldByRecordClassSchema.getMasterIndexField());
        assertEquals("displayName", masterIndexFieldByRecordClassSchema.getDisplayName());
        assertEquals("propertyName", masterIndexFieldByRecordClassSchema.getPropertyName());
        assertEquals("propertyType", masterIndexFieldByRecordClassSchema.getPropertyType());
        assertTrue(masterIndexFieldByRecordClassSchema.getIsMasked());

        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntityArgumentCaptor.capture(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetMasterIndexFieldByRecordClassSchemaPrimaryRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         eq(userAccount));
        assertEquals("2", armRpoExecutionDetailEntityArgumentCaptor.getValue().getSortingField());
        verify(armRpoService).updateArmRpoStatus(eq(armRpoExecutionDetailEntity), eq(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus()), eq(userAccount));
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getMasterIndexFieldByRecordClassSchemaWhereClientThrowsFeignException() {
        // given
        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.getMasterIndexFieldByRecordClassSchema(anyString(), any())).thenThrow(FeignException.class);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () -> armRpoApi.getMasterIndexFieldByRecordClassSchema(
            BEARER_TOKEN, EXECUTION_ID, ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaPrimaryRpoState(), userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM get master index field by record class schema: Unable to get ARM RPO response"));
        verify(armRpoService).updateArmRpoStateAndStatus(eq(armRpoExecutionDetailEntity),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetMasterIndexFieldByRecordClassSchemaPrimaryRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(eq(armRpoExecutionDetailEntity), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), eq(userAccount));
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getMasterIndexFieldByRecordClassSchemaWithNullResponse() {
        // given
        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.getMasterIndexFieldByRecordClassSchema(anyString(), any())).thenReturn(null);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () -> armRpoApi.getMasterIndexFieldByRecordClassSchema(
            BEARER_TOKEN, EXECUTION_ID, ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaPrimaryRpoState(), userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM get master index field by record class schema: Unable to find master index fields in response"));
        verify(armRpoService).updateArmRpoStateAndStatus(eq(armRpoExecutionDetailEntity),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetMasterIndexFieldByRecordClassSchemaPrimaryRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(eq(armRpoExecutionDetailEntity), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), eq(userAccount));
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getMasterIndexFieldByRecordClassSchemaWithEmptyResponse() {
        // given
        MasterIndexFieldByRecordClassSchemaResponse response = new MasterIndexFieldByRecordClassSchemaResponse();
        response.setMasterIndexFields(Collections.emptyList());

        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.getMasterIndexFieldByRecordClassSchema(anyString(), any())).thenReturn(response);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () -> armRpoApi.getMasterIndexFieldByRecordClassSchema(
            BEARER_TOKEN, EXECUTION_ID, ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaPrimaryRpoState(), userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM get master index field by record class schema: Unable to find master index fields in response"));
        verify(armRpoService).updateArmRpoStateAndStatus(eq(armRpoExecutionDetailEntity),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetMasterIndexFieldByRecordClassSchemaPrimaryRpoState()),
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