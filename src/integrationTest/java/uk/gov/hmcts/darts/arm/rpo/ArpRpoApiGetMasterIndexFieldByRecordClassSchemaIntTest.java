package uk.gov.hmcts.darts.arm.rpo;

import feign.FeignException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.MasterIndexFieldByRecordClassSchemaResponse;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.model.rpo.MasterIndexFieldByRecordClassSchema;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ArmRpoStateEnum;
import uk.gov.hmcts.darts.common.enums.ArmRpoStatusEnum;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


class ArpRpoApiGetMasterIndexFieldByRecordClassSchemaIntTest extends IntegrationBase {

    @MockitoBean
    private ArmRpoClient armRpoClient;

    @Autowired
    private ArmRpoApi armRpoApi;


    @Test
    void getMasterIndexFieldByRecordClassSchemaSuccess() {

        // given
        MasterIndexFieldByRecordClassSchemaResponse response = getMasterIndexFieldByRecordClassSchemaResponse("propertyName", "ingestionDate");
        response.setStatus(200);
        response.setIsError(false);
        when(armRpoClient.getMasterIndexFieldByRecordClassSchema(any(), any())).thenReturn(response);

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setCreatedBy(userAccount);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccount);
        var armRpoExecutionDetail = dartsPersistence.save(armRpoExecutionDetailEntity);

        var bearerAuth = "Bearer some-token";

        // when
        List<MasterIndexFieldByRecordClassSchema> result = armRpoApi.getMasterIndexFieldByRecordClassSchema(
            bearerAuth, armRpoExecutionDetail.getId(), ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaPrimaryRpoState(), userAccount);

        // then
        var armRpoExecutionDetailEntityUpdated = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetail.getId()).orElseThrow();
        assertEquals(ArmRpoStateEnum.GET_MASTERINDEXFIELD_BY_RECORDCLASS_SCHEMA_PRIMARY.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.COMPLETED.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoStatus().getId());
        assertEquals("2", armRpoExecutionDetailEntityUpdated.getSortingField());

        MasterIndexFieldByRecordClassSchema masterIndexFieldByRecordClassSchema = result.getFirst();
        assertEquals("1", masterIndexFieldByRecordClassSchema.getMasterIndexField());
        assertEquals("displayName", masterIndexFieldByRecordClassSchema.getDisplayName());
        assertEquals("propertyName", masterIndexFieldByRecordClassSchema.getPropertyName());
        assertEquals("propertyType", masterIndexFieldByRecordClassSchema.getPropertyType());
        assertTrue(masterIndexFieldByRecordClassSchema.getIsMasked());

    }


    @Test
    void getMasterIndexFieldByRecordClassSchemaWhereMasterIndexIsToBeIgnoredAndMissingSortingField() {

        // given
        MasterIndexFieldByRecordClassSchemaResponse response = getMasterIndexFieldByRecordClassSchemaResponse("bf_018", "propertyName");
        response.setStatus(200);
        response.setIsError(false);
        when(armRpoClient.getMasterIndexFieldByRecordClassSchema(any(), any())).thenReturn(response);

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setCreatedBy(userAccount);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccount);
        var armRpoExecutionDetail = dartsPersistence.save(armRpoExecutionDetailEntity);

        var bearerAuth = "Bearer some-token";

        // when
        assertThrows(ArmRpoException.class, () ->
            armRpoApi.getMasterIndexFieldByRecordClassSchema(
                bearerAuth, armRpoExecutionDetail.getId(), ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaPrimaryRpoState(), userAccount));

        // then
        var armRpoExecutionDetailEntityUpdated = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetail.getId()).orElseThrow();
        assertEquals(ArmRpoStateEnum.GET_MASTERINDEXFIELD_BY_RECORDCLASS_SCHEMA_PRIMARY.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.FAILED.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoStatus().getId());

    }

    @Test
    void getMasterIndexFieldByRecordClassSchemaShouldFailIfServerReturnsResponseWithMissingMatterId() {

        // given
        MasterIndexFieldByRecordClassSchemaResponse response = new MasterIndexFieldByRecordClassSchemaResponse();
        response.setMasterIndexFields(List.of());
        response.setStatus(200);
        response.setIsError(false);
        when(armRpoClient.getMasterIndexFieldByRecordClassSchema(any(), any())).thenReturn(response);

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setCreatedBy(userAccount);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccount);
        var armRpoExecutionDetail = dartsPersistence.save(armRpoExecutionDetailEntity);

        var bearerAuth = "Bearer some-token";

        // when
        assertThrows(ArmRpoException.class, () ->
            armRpoApi.getMasterIndexFieldByRecordClassSchema(
                bearerAuth, armRpoExecutionDetail.getId(), ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaPrimaryRpoState(), userAccount));

        // then
        var armRpoExecutionDetailEntityUpdated = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetail.getId()).orElseThrow();
        assertEquals(ArmRpoStateEnum.GET_MASTERINDEXFIELD_BY_RECORDCLASS_SCHEMA_PRIMARY.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.FAILED.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoStatus().getId());
        assertNull(armRpoExecutionDetailEntityUpdated.getMatterId());
    }

    @Test
    void getMasterIndexFieldByRecordClassSchemaFailsWhenClientReturns400Error() {

        // given
        when(armRpoClient.getMasterIndexFieldByRecordClassSchema(any(), any())).thenThrow(FeignException.BadRequest.class);

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setCreatedBy(userAccount);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccount);
        var armRpoExecutionDetail = dartsPersistence.save(armRpoExecutionDetailEntity);

        var bearerAuth = "Bearer some-token";

        // when
        assertThrows(ArmRpoException.class, () -> armRpoApi.getMasterIndexFieldByRecordClassSchema(
            bearerAuth, armRpoExecutionDetail.getId(), ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaPrimaryRpoState(), userAccount));

        // then
        var armRpoExecutionDetailEntityUpdated = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetail.getId()).orElseThrow();
        assertEquals(ArmRpoStateEnum.GET_MASTERINDEXFIELD_BY_RECORDCLASS_SCHEMA_PRIMARY.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.FAILED.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoStatus().getId());
        assertNull(armRpoExecutionDetailEntityUpdated.getMatterId());
    }

    private @NotNull MasterIndexFieldByRecordClassSchemaResponse getMasterIndexFieldByRecordClassSchemaResponse(String propertyName1,
                                                                                                                String propertyName2) {
        MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField masterIndexField1 = new MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField();
        masterIndexField1.setMasterIndexFieldId("1");
        masterIndexField1.setDisplayName("displayName");
        masterIndexField1.setPropertyName(propertyName1);
        masterIndexField1.setPropertyType("propertyType");
        masterIndexField1.setIsMasked(true);

        MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField masterIndexField2 = new MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField();
        masterIndexField2.setMasterIndexFieldId("2");
        masterIndexField2.setDisplayName("displayName");
        masterIndexField2.setPropertyName(propertyName2);
        masterIndexField2.setPropertyType("propertyType");
        masterIndexField2.setIsMasked(false);

        MasterIndexFieldByRecordClassSchemaResponse response = new MasterIndexFieldByRecordClassSchemaResponse();
        response.setMasterIndexFields(List.of(masterIndexField1, masterIndexField2));
        return response;
    }

}
