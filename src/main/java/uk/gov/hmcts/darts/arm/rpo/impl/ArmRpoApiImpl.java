package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.ArmAsyncSearchResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.ExtendedProductionsByMatterResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.ExtendedSearchesByMatterResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.IndexesByMatterIdResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.MasterIndexFieldByRecordClassSchemaRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.MasterIndexFieldByRecordClassSchemaResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.ProfileEntitlementResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.RecordManagementMatterResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.StorageAccountRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.StorageAccountResponse;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.model.rpo.MasterIndexFieldByRecordClassSchema;
import uk.gov.hmcts.darts.arm.rpo.ArmRpoApi;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStateEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
@AllArgsConstructor
@Slf4j
public class ArmRpoApiImpl implements ArmRpoApi {

    private static final String ARM_GET_RECORD_MANAGEMENT_MATTER_ERROR = "Error during ARM get record management matter";
    private static final String IGNORE_MASTER_INDEX_PROPERTY_BF_018 = "bf_018";
    private static final String MASTER_INDEX_FIELD_BY_RECORD_CLASS_SCHEMA_SORTING_FIELD = "ingestionDate";
    private static final String RECORD_CLASS_CODE = "DARTS";
    private static final int FIELD_TYPE_7 = 7;

    private final ArmRpoClient armRpoClient;
    private final ArmRpoService armRpoService;
    private final ArmApiConfigurationProperties armApiConfigurationProperties;

    @Override
    public void getRecordManagementMatter(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, ArmRpoHelper.getRecordManagementMatterRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);

        RecordManagementMatterResponse recordManagementMatterResponse;
        try {
            recordManagementMatterResponse = armRpoClient.getRecordManagementMatter(bearerToken);
        } catch (FeignException e) {
            // this ensures the full error body containing the ARM error detail is logged rather than a truncated version
            log.error("Error during ARM get record management matter: {}", e.contentUTF8());
            throw handleFailureAndCreateException(ARM_GET_RECORD_MANAGEMENT_MATTER_ERROR, armRpoExecutionDetailEntity, userAccount);
        }

        if (recordManagementMatterResponse == null
            || recordManagementMatterResponse.getRecordManagementMatter() == null
            || recordManagementMatterResponse.getRecordManagementMatter().getMatterId() == null) {
            throw handleFailureAndCreateException(ARM_GET_RECORD_MANAGEMENT_MATTER_ERROR, armRpoExecutionDetailEntity, userAccount);
        }

        armRpoExecutionDetailEntity.setMatterId(recordManagementMatterResponse.getRecordManagementMatter().getMatterId());
        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.completedRpoStatus(), userAccount);
    }

    @Override
    public IndexesByMatterIdResponse getIndexesByMatterId(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public void getStorageAccounts(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, ArmRpoHelper.getStorageAccountsRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);
        StringBuilder errorMessage = new StringBuilder("Failure during ARM get storage accounts: ");
        StorageAccountResponse storageAccountResponse;
        try {
            StorageAccountRequest storageAccountRequest = createStorageAccountRequest();
            storageAccountResponse = armRpoClient.getStorageAccounts(bearerToken, storageAccountRequest);

        } catch (FeignException e) {
            // this ensures the full error body containing the ARM error detail is logged rather than a truncated version
            log.error(errorMessage.append("Unable to get ARM RPO response").toString() + " {}", e.contentUTF8());
            throw handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
        }

        if (!nonNull(storageAccountResponse)
            || !CollectionUtils.isNotEmpty(storageAccountResponse.getIndexes())) {
            throw handleFailureAndCreateException(errorMessage.append("Unable to get indexes from storage account response").toString(),
                                                  armRpoExecutionDetailEntity, userAccount);

        }
        String storageAccountName = null;
        for (var index : storageAccountResponse.getIndexes()) {
            if (nonNull(index.getIndex()) && nonNull(index.getIndex().getName())
                && index.getIndex().getName().equals(armApiConfigurationProperties.getArmStorageAccountName())) {
                storageAccountName = index.getIndex().getIndexId();
                break;
            }
        }
        if (!nonNull(storageAccountName)) {
            throw handleFailureAndCreateException(errorMessage.append("Unable to find ARM RPO storage account in response").toString(),
                                                  armRpoExecutionDetailEntity, userAccount);

        }
        armRpoExecutionDetailEntity.setStorageAccountId(storageAccountName);
        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.completedRpoStatus(), userAccount);

    }

    private static StorageAccountRequest createStorageAccountRequest() {
        StorageAccountRequest storageAccountRequest = StorageAccountRequest.builder()
            .onlyKeyAccessType(false)
            .storageType(1)
            .build();
        return storageAccountRequest;
    }

    @Override
    public void getProfileEntitlements(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        final ArmRpoExecutionDetailEntity executionDetail = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(executionDetail,
            ArmRpoHelper.getProfileEntitlementsRpoState(),
            ArmRpoHelper.inProgressRpoStatus(),
            userAccount);

        final StringBuilder exceptionMessageBuilder = new StringBuilder("ARM getProfileEntitlements: ");
        ProfileEntitlementResponse response;
        try {
            response = armRpoClient.getProfileEntitlementResponse(bearerToken);
        } catch (FeignException e) {
            throw handleFailureAndCreateException(exceptionMessageBuilder.append("API call failed: ")
                                                      .append(e)
                                                      .toString(),
                                                  executionDetail, userAccount);
        }

        var entitlements = response.getEntitlements();
        if (CollectionUtils.isEmpty(entitlements)) {
            throw handleFailureAndCreateException(exceptionMessageBuilder.append("No entitlements were returned")
                                                      .toString(),
                                                  executionDetail, userAccount);
        }

        String configuredEntitlement = armApiConfigurationProperties.getArmServiceEntitlement();
        var profileEntitlement = entitlements.stream()
            .filter(entitlement -> configuredEntitlement.equals(entitlement.getName()))
            .findFirst()
            .orElseThrow(() -> handleFailureAndCreateException(exceptionMessageBuilder.append("No matching entitlements were returned")
                                                                   .toString(),
                                                               executionDetail, userAccount));

        String entitlementId = profileEntitlement.getEntitlementId();
        if (StringUtils.isEmpty(entitlementId)) {
            throw handleFailureAndCreateException(exceptionMessageBuilder.append("The obtained entitlement id was empty")
                                                      .toString(),
                                                  executionDetail, userAccount);
        }

        executionDetail.setEntitlementId(entitlementId);
        armRpoService.updateArmRpoStatus(executionDetail, ArmRpoHelper.completedRpoStatus(), userAccount);
    }

    @Override
    public List<MasterIndexFieldByRecordClassSchema> getMasterIndexFieldByRecordClassSchema(String bearerToken,
                                                                                            Integer executionId,
                                                                                            ArmRpoStateEntity rpoStateEntity,
                                                                                            UserAccountEntity userAccount) {
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, rpoStateEntity,
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);

        StringBuilder errorMessage = new StringBuilder("Failure during ARM get master index field by record class schema: ");

        if (!(ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaPrimaryRpoState().getId().equals(rpoStateEntity.getId())
            || ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState().getId().equals(rpoStateEntity.getId()))) {
            errorMessage.append("Invalid state provided - ").append(rpoStateEntity.getDescription());
            throw handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
        }

        MasterIndexFieldByRecordClassSchemaResponse masterIndexFieldByRecordClassSchemaResponse;
        try {
            masterIndexFieldByRecordClassSchemaResponse = armRpoClient.getMasterIndexFieldByRecordClassSchema(
                bearerToken, createMasterIndexFieldByRecordClassSchemaRequest());
        } catch (FeignException e) {
            // this ensures the full error body containing the ARM error detail is logged rather than a truncated version
            log.error(errorMessage.append("Unable to get ARM RPO response").toString() + " {}", e.contentUTF8());
            throw handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
        }

        if (isNull(masterIndexFieldByRecordClassSchemaResponse)
            || CollectionUtils.isEmpty(masterIndexFieldByRecordClassSchemaResponse.getMasterIndexFields())) {
            throw handleFailureAndCreateException(errorMessage.append("Unable to find master index fields in response").toString(),
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
        if (isNull(sortingField)) {
            throw handleFailureAndCreateException(errorMessage.append("Unable to find sorting field in response").toString(),
                                                  armRpoExecutionDetailEntity, userAccount);
        }
        armRpoExecutionDetailEntity.setSortingField(sortingField);
        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.completedRpoStatus(), userAccount);

        return masterIndexFieldByRecordClassSchemaList;
    }

    @Override
    public ArmAsyncSearchResponse addAsyncSearch(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public void saveBackgroundSearch(String bearerToken, Integer executionId, String searchName, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public ExtendedSearchesByMatterResponse getExtendedSearchesByMatter(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public boolean createExportBasedOnSearchResultsTable(String bearerToken, Integer executionId,
                                                         List<MasterIndexFieldByRecordClassSchemaResponse> headerColumns, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public ExtendedProductionsByMatterResponse getExtendedProductionsByMatter(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public List<String> getProductionOutputFiles(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public InputStream downloadProduction(String bearerToken, Integer executionId, String productionExportFileID, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public void removeProduction(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    private ArmRpoException handleFailureAndCreateException(String message,
                                                            ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity,
                                                            UserAccountEntity userAccount) {
        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.failedRpoStatus(), userAccount);
        return new ArmRpoException(message);
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
