package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.ArmAsyncSearchResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.BaseRpoResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.CreateExportBasedOnSearchResultsTableRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.CreateExportBasedOnSearchResultsTableResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.ExtendedProductionsByMatterResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.ExtendedSearchesByMatterResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.IndexesByMatterIdRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.IndexesByMatterIdResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.MasterIndexFieldByRecordClassSchemaRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.MasterIndexFieldByRecordClassSchemaResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.ProductionOutputFilesRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.ProductionOutputFilesResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.ProfileEntitlementResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.RecordManagementMatterResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.RemoveProductionRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.RemoveProductionResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.SaveBackgroundSearchRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.SaveBackgroundSearchResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.StorageAccountRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.StorageAccountResponse;
import uk.gov.hmcts.darts.arm.component.ArmRpoDownloadProduction;
import uk.gov.hmcts.darts.arm.component.impl.AddAsyncSearchRequestGenerator;
import uk.gov.hmcts.darts.arm.component.impl.GetExtendedProductionsByMatterRequestGenerator;
import uk.gov.hmcts.darts.arm.component.impl.GetExtendedSearchesByMatterRequestGenerator;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.model.rpo.MasterIndexFieldByRecordClassSchema;
import uk.gov.hmcts.darts.arm.rpo.ArmRpoApi;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.common.entity.ArmAutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStateEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ArmAutomatedTaskRepository;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.isNull;

@Service
@AllArgsConstructor
@Slf4j
public class ArmRpoApiImpl implements ArmRpoApi {

    private static final String ARM_GET_RECORD_MANAGEMENT_MATTER_ERROR = "Error during ARM get record management matter";
    private static final String IGNORE_MASTER_INDEX_PROPERTY_BF_018 = "bf_018";
    private static final String MASTER_INDEX_FIELD_BY_RECORD_CLASS_SCHEMA_SORTING_FIELD = "ingestionDate";
    private static final String RECORD_CLASS_CODE = "DARTS";
    private static final int FIELD_TYPE_7 = 7;
    private static final String ADD_ASYNC_SEARCH_RELATED_TASK_NAME = "ProcessE2EArmRpoPending";

    private final ArmRpoClient armRpoClient;
    private final ArmRpoService armRpoService;
    private final ArmApiConfigurationProperties armApiConfigurationProperties;
    private final ArmAutomatedTaskRepository armAutomatedTaskRepository;
    private final CurrentTimeHelper currentTimeHelper;
    private final ArmRpoDownloadProduction armRpoDownloadProduction;

    @Override
    public void getRecordManagementMatter(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, ArmRpoHelper.getRecordManagementMatterRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);

        StringBuilder errorMessage = new StringBuilder("Failure during ARM RPO getRecordManagementMatter: ");
        RecordManagementMatterResponse recordManagementMatterResponse;
        try {
            recordManagementMatterResponse = armRpoClient.getRecordManagementMatter(bearerToken);
        } catch (FeignException e) {
            // this ensures the full error body containing the ARM error detail is logged rather than a truncated version
            log.error("Error during ARM get record management matter: {}", e.contentUTF8());
            throw handleFailureAndCreateException(ARM_GET_RECORD_MANAGEMENT_MATTER_ERROR, armRpoExecutionDetailEntity, userAccount);
        }

        handleResponseStatus(userAccount, recordManagementMatterResponse, errorMessage, armRpoExecutionDetailEntity);

        if (isNull(recordManagementMatterResponse.getRecordManagementMatter())
            || StringUtils.isBlank(recordManagementMatterResponse.getRecordManagementMatter().getMatterId())) {
            throw handleFailureAndCreateException(ARM_GET_RECORD_MANAGEMENT_MATTER_ERROR, armRpoExecutionDetailEntity, userAccount);
        }

        armRpoExecutionDetailEntity.setMatterId(recordManagementMatterResponse.getRecordManagementMatter().getMatterId());
        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.completedRpoStatus(), userAccount);
    }

    @Override
    public void getIndexesByMatterId(String bearerToken, Integer executionId, String matterId, UserAccountEntity userAccount) {
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, ArmRpoHelper.getIndexesByMatterIdRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);

        StringBuilder errorMessage = new StringBuilder("Failure during ARM RPO get indexes by matter ID: ");
        IndexesByMatterIdResponse indexesByMatterIdResponse;
        try {
            indexesByMatterIdResponse = armRpoClient.getIndexesByMatterId(bearerToken, createIndexesByMatterIdRequest(matterId));
        } catch (FeignException e) {
            // this ensures the full error body containing the ARM error detail is logged rather than a truncated version
            log.error(errorMessage.append("Unable to get ARM RPO response") + " {}", e.contentUTF8());
            throw handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
        }

        handleResponseStatus(userAccount, indexesByMatterIdResponse, errorMessage, armRpoExecutionDetailEntity);

        if (CollectionUtils.isEmpty(indexesByMatterIdResponse.getIndexes())
            || isNull(indexesByMatterIdResponse.getIndexes().getFirst())
            || isNull(indexesByMatterIdResponse.getIndexes().getFirst().getIndex())
            || StringUtils.isBlank(indexesByMatterIdResponse.getIndexes().getFirst().getIndex().getIndexId())) {
            throw handleFailureAndCreateException(errorMessage.append("Unable to find indexes by matter ID in response").toString(),
                                                  armRpoExecutionDetailEntity,
                                                  userAccount);
        }
        if (indexesByMatterIdResponse.getIndexes().size() > 1) {
            log.warn("More than one index found in response for matterId: {} - response {}", matterId, indexesByMatterIdResponse);
        }
        armRpoExecutionDetailEntity.setIndexId(indexesByMatterIdResponse.getIndexes().getFirst().getIndex().getIndexId());
        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.completedRpoStatus(), userAccount);
    }

    private IndexesByMatterIdRequest createIndexesByMatterIdRequest(String matterId) {
        return IndexesByMatterIdRequest.builder()
            .matterId(matterId)
            .build();
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

        handleResponseStatus(userAccount, storageAccountResponse, errorMessage, armRpoExecutionDetailEntity);

        if (!CollectionUtils.isNotEmpty(storageAccountResponse.getDataDetails())) {
            throw handleFailureAndCreateException(errorMessage.append("No data details were present in the storage account response").toString(),
                                                  armRpoExecutionDetailEntity, userAccount);
        }

        final String armStorageAccountName = armApiConfigurationProperties.getArmStorageAccountName();
        List<String> storageAccountIds = storageAccountResponse.getDataDetails().stream()
            .filter(dataDetails -> armStorageAccountName.equals(dataDetails.getName()))
            .map(StorageAccountResponse.DataDetails::getId)
            .filter(id -> !StringUtils.isBlank(id))
            .toList();

        if (CollectionUtils.isEmpty(storageAccountIds)) {
            throw handleFailureAndCreateException(errorMessage.append("Unable to find ARM RPO storage account in response").toString(),
                                                  armRpoExecutionDetailEntity, userAccount);
        }

        String accountId = storageAccountIds.getFirst();
        if (storageAccountIds.size() > 1) {
            log.warn("More than one storage account id found in response for account name: {}. Assuming the first id is correct: {}. Response {}",
                     armStorageAccountName, accountId, storageAccountResponse);
        }

        armRpoExecutionDetailEntity.setStorageAccountId(accountId);
        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.completedRpoStatus(), userAccount);
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
        handleResponseStatus(userAccount, response, exceptionMessageBuilder, executionDetail);

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
        if (StringUtils.isBlank(sortingField)) {
            throw handleFailureAndCreateException(errorMessage.append("Unable to find sorting field in response").toString(),
                                                  armRpoExecutionDetailEntity, userAccount);
        }
        armRpoExecutionDetailEntity.setSortingField(sortingField);
        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.completedRpoStatus(), userAccount);

        return masterIndexFieldByRecordClassSchemaList;
    }

    @Override
    public String addAsyncSearch(String bearerToken, Integer executionId, UserAccountEntity userAccount) {

        final ArmRpoExecutionDetailEntity executionDetail = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(executionDetail,
                                                 ArmRpoHelper.addAsyncSearchRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(),
                                                 userAccount);

        OffsetDateTime now = currentTimeHelper.currentOffsetDateTime();
        String searchName = "DARTS_RPO_%s".formatted(
            now.format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss"))
        );

        final StringBuilder exceptionMessageBuilder = new StringBuilder("ARM addAsyncSearch: ");
        ArmAutomatedTaskEntity armAutomatedTaskEntity = armAutomatedTaskRepository.findByAutomatedTask_taskName(ADD_ASYNC_SEARCH_RELATED_TASK_NAME)
            .orElseThrow(() -> handleFailureAndCreateException(exceptionMessageBuilder.append("Automated task not found: ")
                                                                   .append(ADD_ASYNC_SEARCH_RELATED_TASK_NAME)
                                                                   .toString(),
                                                               executionDetail, userAccount));

        AddAsyncSearchRequestGenerator requestGenerator;
        try {
            requestGenerator = createAddAsyncSearchRequestGenerator(searchName, executionDetail, armAutomatedTaskEntity, now);
        } catch (NullPointerException e) {
            throw handleFailureAndCreateException(exceptionMessageBuilder.append("Could not construct API request: ")
                                                      .append(e)
                                                      .toString(),
                                                  executionDetail, userAccount);
        }

        ArmAsyncSearchResponse response;
        try {
            response = armRpoClient.addAsyncSearch(bearerToken, requestGenerator.getJsonRequest());
        } catch (FeignException e) {
            throw handleFailureAndCreateException(exceptionMessageBuilder.append("API call failed: ")
                                                      .append(e)
                                                      .toString(),
                                                  executionDetail, userAccount);
        }
        handleResponseStatus(userAccount, response, exceptionMessageBuilder, executionDetail);

        String searchId = response.getSearchId();
        if (searchId == null) {
            throw handleFailureAndCreateException(exceptionMessageBuilder.append("The obtained search id was empty")
                                                      .toString(),
                                                  executionDetail, userAccount);
        }

        executionDetail.setSearchId(searchId);
        armRpoService.updateArmRpoStatus(executionDetail, ArmRpoHelper.completedRpoStatus(), userAccount);

        return searchName;
    }


    @Override
    public void saveBackgroundSearch(String bearerToken, Integer executionId, String searchName, UserAccountEntity userAccount) {
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, ArmRpoHelper.saveBackgroundSearchRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);

        StringBuilder errorMessage = new StringBuilder("Failure during ARM save background search: ");
        SaveBackgroundSearchResponse saveBackgroundSearchResponse;
        try {
            SaveBackgroundSearchRequest saveBackgroundSearchRequest =
                createSaveBackgroundSearchRequest(searchName, armRpoExecutionDetailEntity.getSearchId());
            saveBackgroundSearchResponse = armRpoClient.saveBackgroundSearch(bearerToken, saveBackgroundSearchRequest);
        } catch (FeignException e) {
            // this ensures the full error body containing the ARM error detail is logged rather than a truncated version
            log.error(errorMessage.append("Unable to save background search").toString() + " {}", e.contentUTF8());
            throw handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
        }

        handleResponseStatus(userAccount, saveBackgroundSearchResponse, errorMessage, armRpoExecutionDetailEntity);

        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.completedRpoStatus(), userAccount);
    }

    @Override
    public void getExtendedSearchesByMatter(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, ArmRpoHelper.getExtendedSearchesByMatterRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);

        StringBuilder errorMessage = new StringBuilder("Failure during ARM RPO getExtendedSearchesByMatter: ");
        GetExtendedSearchesByMatterRequestGenerator requestGenerator;
        try {
            requestGenerator = createExtendedSearchesByMatterRequestGenerator(armRpoExecutionDetailEntity.getMatterId());
        } catch (NullPointerException e) {
            throw handleFailureAndCreateException(errorMessage.append("Could not construct API request: ").append(e)
                                                      .toString(), armRpoExecutionDetailEntity, userAccount);
        }

        ExtendedSearchesByMatterResponse extendedSearchesByMatterResponse;
        try {
            extendedSearchesByMatterResponse = armRpoClient.getExtendedSearchesByMatter(bearerToken, requestGenerator.getJsonRequest());
        } catch (FeignException e) {
            // this ensures the full error body containing the ARM error detail is logged rather than a truncated version
            log.error(errorMessage.append("Unable to get ARM RPO response").toString() + " {}", e.contentUTF8());
            throw handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
        }

        handleResponseStatus(userAccount, extendedSearchesByMatterResponse, errorMessage, armRpoExecutionDetailEntity);

        if (isNull(extendedSearchesByMatterResponse.getSearches())
            || CollectionUtils.isEmpty(extendedSearchesByMatterResponse.getSearches())
            || isNull(extendedSearchesByMatterResponse.getSearches().getFirst())
            || isNull(extendedSearchesByMatterResponse.getSearches().getFirst().getSearch())
            || isNull(extendedSearchesByMatterResponse.getSearches().getFirst().getSearch().getTotalCount())) {
            throw handleFailureAndCreateException(errorMessage.append("Search item count is missing").toString(),
                                                  armRpoExecutionDetailEntity, userAccount);
        }

        armRpoExecutionDetailEntity.setSearchItemCount(extendedSearchesByMatterResponse.getSearches().getFirst().getSearch().getTotalCount());
        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.completedRpoStatus(), userAccount);
    }


    @Override
    public boolean createExportBasedOnSearchResultsTable(String bearerToken, Integer executionId,
                                                         List<MasterIndexFieldByRecordClassSchema> headerColumns, UserAccountEntity userAccount) {
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, ArmRpoHelper.createExportBasedOnSearchResultsTableRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);

        StringBuilder errorMessage = new StringBuilder("Failure during ARM createExportBasedOnSearchResultsTable: ");
        CreateExportBasedOnSearchResultsTableRequest request;
        try {
            request = createRequestForCreateExportBasedOnSearchResultsTable(
                headerColumns, armRpoExecutionDetailEntity.getSearchId(),
                armRpoExecutionDetailEntity.getSearchItemCount(),
                armRpoExecutionDetailEntity.getProductionId(),
                armRpoExecutionDetailEntity.getStorageAccountId()
            );
        } catch (NullPointerException e) {
            throw handleFailureAndCreateException(errorMessage.append("Could not construct API request: ").append(e).toString(),
                                                  armRpoExecutionDetailEntity, userAccount);
        }
        CreateExportBasedOnSearchResultsTableResponse response;
        try {

            response = armRpoClient.createExportBasedOnSearchResultsTable(bearerToken, request);
        } catch (FeignException e) {
            // this ensures the full error body containing the ARM error detail is logged rather than a truncated version
            log.error(errorMessage.append("Unable to get ARM RPO response").toString() + " {}",
                      e.contentUTF8());
            throw handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
        }
        if (isNull(response) || isNull(response.getStatus()) || isNull(response.getIsError())
            || (!response.getIsError() && isNull(response.getResponseStatus()))
        ) {
            throw handleFailureAndCreateException(errorMessage.append("ARM RPO API response is invalid - ").append(response)
                                                      .toString(),
                                                  armRpoExecutionDetailEntity, userAccount);
        }
        try {
            HttpStatus responseStatus = HttpStatus.valueOf(response.getStatus());
            if (HttpStatus.BAD_REQUEST.value() == responseStatus.value()) {
                if (response.getResponseStatus() == 2) {
                    log.error("The search is still running and cannot export as csv - {}", response);
                    return false;
                } else {
                    throw handleFailureAndCreateException(errorMessage.append("ARM RPO API failed with invalid status - ").append(responseStatus)
                                                              .append(" and response - ").append(response).toString(),
                                                          armRpoExecutionDetailEntity, userAccount);
                }
            } else if (!responseStatus.is2xxSuccessful() || response.getIsError()) {
                throw handleFailureAndCreateException(errorMessage.append("ARM RPO API failed with status - ").append(responseStatus)
                                                          .append(" and response - ").append(response).toString(),
                                                      armRpoExecutionDetailEntity, userAccount);
            }
        } catch (IllegalArgumentException e) {
            throw handleFailureAndCreateException(errorMessage.append("ARM RPO API response status is invalid - ")
                                                      .append(response).toString(),
                                                  armRpoExecutionDetailEntity, userAccount);
        }
        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.completedRpoStatus(), userAccount);
        return true;
    }

    @Override
    public void getExtendedProductionsByMatter(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, ArmRpoHelper.getExtendedProductionsByMatterRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);

        StringBuilder errorMessage = new StringBuilder("Failure during ARM RPO Extended Productions By Matter: ");

        GetExtendedProductionsByMatterRequestGenerator requestGenerator;
        try {
            requestGenerator = createExtendedProductionsByMatterRequest(armRpoExecutionDetailEntity.getMatterId());
        } catch (NullPointerException e) {
            throw handleFailureAndCreateException(errorMessage.append("Could not construct API request: ").append(e)
                                                      .toString(), armRpoExecutionDetailEntity, userAccount);
        }

        ExtendedProductionsByMatterResponse extendedProductionsByMatterResponse;
        try {
            extendedProductionsByMatterResponse = armRpoClient.getExtendedProductionsByMatter(bearerToken, requestGenerator.getJsonRequest());
        } catch (FeignException e) {
            // this ensures the full error body containing the ARM error detail is logged rather than a truncated version
            log.error(errorMessage.append("Unable to get ARM RPO response").toString() + " {}", e.contentUTF8());
            throw handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
        }

        handleResponseStatus(userAccount, extendedProductionsByMatterResponse, errorMessage, armRpoExecutionDetailEntity);
        if (isNull(extendedProductionsByMatterResponse.getProductions())
            || CollectionUtils.isEmpty(extendedProductionsByMatterResponse.getProductions())
            || isNull(extendedProductionsByMatterResponse.getProductions().getFirst())
            || StringUtils.isBlank(extendedProductionsByMatterResponse.getProductions().getFirst().getProductionId())) {
            throw handleFailureAndCreateException(errorMessage.append("ProductionId is missing from ARM RPO response").toString(),
                                                  armRpoExecutionDetailEntity, userAccount);
        }

        armRpoExecutionDetailEntity.setProductionId(extendedProductionsByMatterResponse.getProductions().getFirst().getProductionId());
        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.completedRpoStatus(), userAccount);
    }

    @Override
    public List<String> getProductionOutputFiles(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        final ArmRpoExecutionDetailEntity executionDetail = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(executionDetail,
                                                 ArmRpoHelper.getProductionOutputFilesRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(),
                                                 userAccount);

        final StringBuilder exceptionMessageBuilder = new StringBuilder("ARM getProductionOutputFiles: ");

        String productionId = executionDetail.getProductionId();
        if (StringUtils.isBlank(productionId)) {
            throw handleFailureAndCreateException(exceptionMessageBuilder.append("production id was blank for execution id: ")
                                                      .append(executionId)
                                                      .toString(),
                                                  executionDetail, userAccount);
        }

        ProductionOutputFilesResponse response;
        try {
            response = armRpoClient.getProductionOutputFiles(bearerToken, createProductionOutputFilesRequest(productionId));
        } catch (FeignException e) {
            throw handleFailureAndCreateException(exceptionMessageBuilder.append("API call failed: ")
                                                      .append(e)
                                                      .toString(),
                                                  executionDetail, userAccount);
        }
        handleResponseStatus(userAccount, response, exceptionMessageBuilder, executionDetail);

        List<ProductionOutputFilesResponse.ProductionExportFile> productionExportFiles = response.getProductionExportFiles();
        if (CollectionUtils.isEmpty(productionExportFiles)) {
            throw handleFailureAndCreateException(exceptionMessageBuilder.append("No production export files were returned")
                                                      .toString(),
                                                  executionDetail, userAccount);
        }

        List<String> productionExportFileIds = productionExportFiles.stream()
            .filter(Objects::nonNull)
            .map(ProductionOutputFilesResponse.ProductionExportFile::getProductionExportFileDetails)
            .filter(Objects::nonNull)
            .map(ProductionOutputFilesResponse.ProductionExportFileDetail::getProductionExportFileId)
            .filter(StringUtils::isNotBlank)
            .toList();

        if (productionExportFileIds.isEmpty()) {
            throw handleFailureAndCreateException(exceptionMessageBuilder.append("No production export file ids were returned")
                                                      .toString(),
                                                  executionDetail, userAccount);
        }

        armRpoService.updateArmRpoStatus(executionDetail, ArmRpoHelper.completedRpoStatus(), userAccount);

        return productionExportFileIds;
    }

    @Override
    public InputStream downloadProduction(String bearerToken, Integer executionId, String productionExportFileId,
                                          UserAccountEntity userAccount) throws IOException {
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, ArmRpoHelper.downloadProductionRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);

        feign.Response response;
        StringBuilder errorMessage = new StringBuilder("Failure during download production: ");

        try {
            response = armRpoDownloadProduction.downloadProduction(bearerToken, productionExportFileId);
        } catch (FeignException e) {
            // this ensures the full error body containing the ARM error detail is logged rather than a truncated version
            log.error(errorMessage.append("Error during ARM RPO download production id: ").append(productionExportFileId)
                          .toString() + " {}", e.contentUTF8());
            throw handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
        }

        // on any error occurring return a download failure
        if (isNull(response) || isNull(response.status()) || !HttpStatus.valueOf(response.status()).is2xxSuccessful()) {
            errorMessage.append("Failed ARM RPO download production with id: ").append(productionExportFileId)
                .append(" response ").append(response);
            log.error(errorMessage.toString());
            throw handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
        }

        log.debug("Successfully downloaded ARM data for productionExportFileId: {}", productionExportFileId);
        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.completedRpoStatus(), userAccount);
        return response.body().asInputStream();
    }

    @Override
    public void removeProduction(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, ArmRpoHelper.removeProductionRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);

        StringBuilder errorMessage = new StringBuilder("Failure during ARM RPO removeProduction: ");
        RemoveProductionResponse removeProductionResponse = null;
        try {
            RemoveProductionRequest request = createRemoveProductionRequest(armRpoExecutionDetailEntity);
            removeProductionResponse = armRpoClient.removeProduction(bearerToken, request);
        } catch (FeignException e) {
            // this ensures the full error body containing the ARM error detail is logged rather than a truncated version
            log.error(errorMessage.append("Unable to get ARM RPO response ").append(removeProductionResponse).toString() + " {}", e.contentUTF8());
            throw handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
        }

        handleResponseStatus(userAccount, removeProductionResponse, errorMessage, armRpoExecutionDetailEntity);

        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.completedRpoStatus(), userAccount);
    }

    private RemoveProductionRequest createRemoveProductionRequest(ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
        return RemoveProductionRequest.builder()
            .productionId(armRpoExecutionDetailEntity.getProductionId())
            .deleteSearch(true)
            .build();
    }

    private GetExtendedProductionsByMatterRequestGenerator createExtendedProductionsByMatterRequest(String matterId) {
        return GetExtendedProductionsByMatterRequestGenerator.builder()
            .matterId(matterId)
            .build();
    }

    private ArmRpoException handleFailureAndCreateException(String message,
                                                            ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity,
                                                            UserAccountEntity userAccount) {
        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.failedRpoStatus(), userAccount);
        return new ArmRpoException(message);
    }

    private void handleResponseStatus(UserAccountEntity userAccount, BaseRpoResponse baseRpoResponse, StringBuilder errorMessage,
                                      ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
        if (isNull(baseRpoResponse)
            || isNull(baseRpoResponse.getStatus())
            || isNull(baseRpoResponse.getIsError())) {
            throw handleFailureAndCreateException(errorMessage.append("ARM RPO API response is invalid - ").append(baseRpoResponse)
                                                      .toString(),
                                                  armRpoExecutionDetailEntity, userAccount);
        }
        try {
            HttpStatus responseStatus = HttpStatus.valueOf(baseRpoResponse.getStatus());
            if (!responseStatus.is2xxSuccessful() || baseRpoResponse.getIsError()) {
                throw handleFailureAndCreateException(errorMessage.append("ARM RPO API failed with status - ").append(responseStatus)
                                                          .append(" and response - ").append(baseRpoResponse).toString(),
                                                      armRpoExecutionDetailEntity, userAccount);
            }
        } catch (IllegalArgumentException e) {
            throw handleFailureAndCreateException(errorMessage.append("ARM RPO API response status is invalid - ")
                                                      .append(baseRpoResponse).toString(),
                                                  armRpoExecutionDetailEntity, userAccount);
        }
    }

    private StorageAccountRequest createStorageAccountRequest() {
        return StorageAccountRequest.builder()
            .onlyKeyAccessType(false)
            .storageType(1)
            .build();
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

    private AddAsyncSearchRequestGenerator createAddAsyncSearchRequestGenerator(String searchName,
                                                                                ArmRpoExecutionDetailEntity executionDetail,
                                                                                ArmAutomatedTaskEntity armAutomatedTaskEntity,
                                                                                OffsetDateTime now) throws NullPointerException {
        return AddAsyncSearchRequestGenerator.builder()
            .name(searchName)
            .searchName(searchName)
            .matterId(executionDetail.getMatterId())
            .entitlementId(executionDetail.getEntitlementId())
            .indexId(executionDetail.getIndexId())
            .sortingField(executionDetail.getSortingField())
            .startTime(now.minusHours(armAutomatedTaskEntity.getRpoCsvEndHour()))
            .endTime(now.minusHours(armAutomatedTaskEntity.getRpoCsvStartHour()))
            .build();
    }

    private CreateExportBasedOnSearchResultsTableRequest createRequestForCreateExportBasedOnSearchResultsTable(
        List<MasterIndexFieldByRecordClassSchema> headerColumns, String searchId, int searchItemsCount, String productionName,
        String storageAccountId) {

        return CreateExportBasedOnSearchResultsTableRequest.builder()
            .core(null)
            .formFields(null)
            .searchId(searchId)
            .searchitemsCount(searchItemsCount)
            .headerColumns(createHeaderColumnsFromMasterIndexFieldByRecordClassSchemaResponse(headerColumns))
            .productionName(productionName)
            .storageAccountId(storageAccountId)
            .build();
    }

    private SaveBackgroundSearchRequest createSaveBackgroundSearchRequest(String searchName, String searchId) {
        return SaveBackgroundSearchRequest.builder()
            .name(searchName)
            .searchId(searchId)
            .build();
    }

    private GetExtendedSearchesByMatterRequestGenerator createExtendedSearchesByMatterRequestGenerator(String matterId) {
        return GetExtendedSearchesByMatterRequestGenerator.builder()
            .matterId(matterId)
            .build();
    }

    private List<CreateExportBasedOnSearchResultsTableRequest.HeaderColumn> createHeaderColumnsFromMasterIndexFieldByRecordClassSchemaResponse(
        List<MasterIndexFieldByRecordClassSchema> masterIndexFieldByRecordClassSchemas) {

        List<CreateExportBasedOnSearchResultsTableRequest.HeaderColumn> headerColumnList = new ArrayList<>();
        for (MasterIndexFieldByRecordClassSchema masterIndexField : masterIndexFieldByRecordClassSchemas) {
            headerColumnList.add(createHeaderColumn(masterIndexField));
        }
        return headerColumnList;
    }

    private CreateExportBasedOnSearchResultsTableRequest.HeaderColumn createHeaderColumn(
        MasterIndexFieldByRecordClassSchema masterIndexField) {
        return CreateExportBasedOnSearchResultsTableRequest.HeaderColumn.builder()
            .masterIndexField(masterIndexField.getMasterIndexField())
            .displayName(masterIndexField.getDisplayName())
            .propertyName(masterIndexField.getPropertyName())
            .propertyType(masterIndexField.getPropertyType())
            .isMasked(masterIndexField.getIsMasked())
            .build();
    }

    private ProductionOutputFilesRequest createProductionOutputFilesRequest(String productionId) {
        return ProductionOutputFilesRequest.builder()
            .productionId(productionId)
            .build();
    }

}
