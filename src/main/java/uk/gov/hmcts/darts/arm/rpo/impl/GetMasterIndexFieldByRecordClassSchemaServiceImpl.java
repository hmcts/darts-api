package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.MasterIndexFieldByRecordClassSchemaRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.MasterIndexFieldByRecordClassSchemaResponse;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.model.rpo.MasterIndexFieldByRecordClassSchema;
import uk.gov.hmcts.darts.arm.rpo.GetMasterIndexFieldByRecordClassSchemaService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStateEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;

@Service
@AllArgsConstructor
@Slf4j
public class GetMasterIndexFieldByRecordClassSchemaServiceImpl implements GetMasterIndexFieldByRecordClassSchemaService {

    private static final String IGNORE_MASTER_INDEX_PROPERTY_BF_018 = "bf_018";
    private static final String MASTER_INDEX_FIELD_BY_RECORD_CLASS_SCHEMA_SORTING_FIELD = "ingestionDate";
    private static final String RECORD_CLASS_CODE = "DARTS";
    private static final int FIELD_TYPE_7 = 7;

    private final ArmRpoClient armRpoClient;
    private final ArmRpoService armRpoService;
    private final ArmRpoUtil armRpoUtil;

    @Override
    public List<MasterIndexFieldByRecordClassSchema> getMasterIndexFieldByRecordClassSchema(String bearerToken,
                                                                                            Integer executionId,
                                                                                            ArmRpoStateEntity rpoStateEntity,
                                                                                            UserAccountEntity userAccount) {
        log.debug("getMasterIndexFieldByRecordClassSchema called with executionId: {}, rpo state: {}", executionId, rpoStateEntity.getId());
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, rpoStateEntity,
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);

        StringBuilder errorMessage = new StringBuilder(273).append("Failure during ARM get master index field by record class schema: ");

        if (!(ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaPrimaryRpoState().getId().equals(rpoStateEntity.getId())
            || ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState().getId().equals(rpoStateEntity.getId()))) {
            errorMessage.append("Invalid state provided - ").append(rpoStateEntity.getDescription());
            throw armRpoUtil.handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
        }

        MasterIndexFieldByRecordClassSchemaResponse masterIndexFieldByRecordClassSchemaResponse;
        try {
            masterIndexFieldByRecordClassSchemaResponse = armRpoClient.getMasterIndexFieldByRecordClassSchema(
                bearerToken, createMasterIndexFieldByRecordClassSchemaRequest());
        } catch (FeignException e) {
            log.error(errorMessage.append(armRpoUtil.UNABLE_TO_GET_ARM_RPO_RESPONSE).append(e).toString(), e);
            throw armRpoUtil.handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
        }
        log.debug("ARM RPO Response - MasterIndexFieldByRecordClassSchemaResponse: {}", masterIndexFieldByRecordClassSchemaResponse);
        return processMasterIndexFieldByRecordClassSchemas(userAccount, masterIndexFieldByRecordClassSchemaResponse, errorMessage, armRpoExecutionDetailEntity);
    }

    private List<MasterIndexFieldByRecordClassSchema> processMasterIndexFieldByRecordClassSchemas(
        UserAccountEntity userAccount, MasterIndexFieldByRecordClassSchemaResponse masterIndexFieldByRecordClassSchemaResponse,
        StringBuilder errorMessage, ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {

        if (isNull(masterIndexFieldByRecordClassSchemaResponse)
            || CollectionUtils.isEmpty(masterIndexFieldByRecordClassSchemaResponse.getMasterIndexFields())) {
            throw armRpoUtil.handleFailureAndCreateException(errorMessage.append("Unable to find master index fields in response").toString(),
                                                             armRpoExecutionDetailEntity,
                                                             userAccount);
        }
        List<MasterIndexFieldByRecordClassSchema> masterIndexFieldByRecordClassSchemaList = new ArrayList<>();
        String sortingField = null;
        for (var masterIndexField : masterIndexFieldByRecordClassSchemaResponse.getMasterIndexFields()) {
            //ignore master index property bf_018
            if (!IGNORE_MASTER_INDEX_PROPERTY_BF_018.equals(masterIndexField.getPropertyName())) {
                // get sorting field index id
                if (MASTER_INDEX_FIELD_BY_RECORD_CLASS_SCHEMA_SORTING_FIELD.equals(masterIndexField.getPropertyName())) {
                    sortingField = masterIndexField.getMasterIndexFieldId();
                }
                masterIndexFieldByRecordClassSchemaList.add(createMasterIndexFieldByRecordClassSchema(masterIndexField));
            }
        }
        if (StringUtils.isBlank(sortingField)) {
            throw armRpoUtil.handleFailureAndCreateException(errorMessage.append("Unable to find sorting field in response").toString(),
                                                             armRpoExecutionDetailEntity, userAccount);
        }
        armRpoExecutionDetailEntity.setSortingField(sortingField);
        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.completedRpoStatus(), userAccount);

        return masterIndexFieldByRecordClassSchemaList;
    }

    private MasterIndexFieldByRecordClassSchemaRequest createMasterIndexFieldByRecordClassSchemaRequest() {
        return MasterIndexFieldByRecordClassSchemaRequest.builder()
            .recordClassCode(RECORD_CLASS_CODE)
            .isForSearch(true)
            .fieldType(FIELD_TYPE_7)
            .usePaging(false)
            .build();
    }

    private MasterIndexFieldByRecordClassSchema createMasterIndexFieldByRecordClassSchema(
        MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField masterIndexField) {
        return MasterIndexFieldByRecordClassSchema.builder()
            .masterIndexField(masterIndexField.getMasterIndexFieldId())
            .displayName(masterIndexField.getDisplayName())
            .propertyName(masterIndexField.getPropertyName())
            .propertyType(masterIndexField.getPropertyType())
            .isMasked(masterIndexField.getIsMasked())
            .build();
    }
}
