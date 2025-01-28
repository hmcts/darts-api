package uk.gov.hmcts.darts.arm.rpo.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.gov.hmcts.darts.arm.client.model.rpo.EmptyRpoRequest;
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
import uk.gov.hmcts.darts.arm.exception.ArmRpoInProgressException;
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
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.arm.enums.ArmRpoResponseStatusCode.IN_PROGRESS_STATUS;
import static uk.gov.hmcts.darts.arm.enums.ArmRpoResponseStatusCode.READY_STATUS;

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
    private static final String UNABLE_TO_GET_ARM_RPO_RESPONSE = "Unable to get ARM RPO response from client ";

    private static final String COULD_NOT_CONSTRUCT_API_REQUEST = "Could not construct API request: ";
    private static final String AND_RESPONSE = " and response - ";
    public static final int CREATE_EXPORT_BASED_ON_SEARCH_RESULTS_IN_PROGRESS_STATUS = 2;

    private final ArmRpoClient armRpoClient;
    private final ArmRpoService armRpoService;
    private final ArmApiConfigurationProperties armApiConfigurationProperties;
    private final ArmAutomatedTaskRepository armAutomatedTaskRepository;
    private final CurrentTimeHelper currentTimeHelper;
    private final ArmRpoDownloadProduction armRpoDownloadProduction;
    private final ObjectMapper objectMapper;

    @Override
    public void getRecordManagementMatter(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        log.debug("getRecordManagementMatter called with executionId: {}", executionId);
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, ArmRpoHelper.getRecordManagementMatterRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);

        StringBuilder errorMessage = new StringBuilder(96).append("Failure during ARM RPO getRecordManagementMatter: ");
        RecordManagementMatterResponse recordManagementMatterResponse;
        try {
            EmptyRpoRequest emptyRpoRequest = EmptyRpoRequest.builder().build();
            recordManagementMatterResponse = armRpoClient.getRecordManagementMatter(bearerToken, emptyRpoRequest);
        } catch (FeignException e) {
            log.error(errorMessage.append(UNABLE_TO_GET_ARM_RPO_RESPONSE).append(e).toString(), e);
            throw handleFailureAndCreateException(ARM_GET_RECORD_MANAGEMENT_MATTER_ERROR, armRpoExecutionDetailEntity, userAccount);
        }
        log.debug("ARM RPO Response - RecordManagementMatterResponse: {}", recordManagementMatterResponse);
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
        log.debug("getIndexesByMatterId called with executionId: {}, matterId: {}", executionId, matterId);
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, ArmRpoHelper.getIndexesByMatterIdRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);

        StringBuilder errorMessage = new StringBuilder(151).append("Failure during ARM RPO get indexes by matter ID: ");
        IndexesByMatterIdResponse indexesByMatterIdResponse;
        try {
            indexesByMatterIdResponse = armRpoClient.getIndexesByMatterId(bearerToken, createIndexesByMatterIdRequest(matterId));
        } catch (FeignException e) {
            log.error(errorMessage.append(UNABLE_TO_GET_ARM_RPO_RESPONSE).append(e).toString(), e);
            throw handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
        }

        processIndexesByMatterIdResponse(matterId, userAccount, indexesByMatterIdResponse, errorMessage, armRpoExecutionDetailEntity);
    }

    private void processIndexesByMatterIdResponse(String matterId, UserAccountEntity userAccount, IndexesByMatterIdResponse indexesByMatterIdResponse,
                                                  StringBuilder errorMessage,
                                                  ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
        handleResponseStatus(userAccount, indexesByMatterIdResponse, errorMessage, armRpoExecutionDetailEntity);

        List<IndexesByMatterIdResponse.Index> indexes = indexesByMatterIdResponse.getIndexes();
        if (CollectionUtils.isEmpty(indexes)
            || isNull(indexes.getFirst())
            || isNull(indexes.getFirst().getIndex())
            || StringUtils.isBlank(indexes.getFirst().getIndex().getIndexId())) {
            throw handleFailureAndCreateException(errorMessage.append("Unable to find any indexes by matter ID in response").toString(),
                                                  armRpoExecutionDetailEntity,
                                                  userAccount);
        }

        String indexId = indexes.getFirst().getIndex().getIndexId();
        if (indexes.size() > 1) {
            log.warn("More than one index found in response for matterId: {}. Using first index id: {} from response: {}",
                     matterId, indexId, indexesByMatterIdResponse);
        }
        armRpoExecutionDetailEntity.setIndexId(indexId);
        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.completedRpoStatus(), userAccount);
    }

    @Override
    public void getStorageAccounts(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        log.debug("getStorageAccounts called with executionId: {}", executionId);
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, ArmRpoHelper.getStorageAccountsRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);
        StringBuilder errorMessage = new StringBuilder("Failure during ARM get storage accounts: ");
        StorageAccountResponse storageAccountResponse;
        try {
            StorageAccountRequest storageAccountRequest = createStorageAccountRequest();
            storageAccountResponse = armRpoClient.getStorageAccounts(bearerToken, storageAccountRequest);

        } catch (FeignException e) {
            log.error(errorMessage.append(UNABLE_TO_GET_ARM_RPO_RESPONSE).append(e).toString(), e);
            throw handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
        }

        processGetStorageAccountsResponse(userAccount, storageAccountResponse, errorMessage, armRpoExecutionDetailEntity);
    }

    private void processGetStorageAccountsResponse(UserAccountEntity userAccount, StorageAccountResponse storageAccountResponse, StringBuilder errorMessage,
                                                   ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
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
        log.debug("getProfileEntitlements called with executionId: {}", executionId);
        final ArmRpoExecutionDetailEntity executionDetail = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(executionDetail,
                                                 ArmRpoHelper.getProfileEntitlementsRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(),
                                                 userAccount);

        final StringBuilder exceptionMessageBuilder = new StringBuilder(90).append("ARM getProfileEntitlements: ");
        ProfileEntitlementResponse profileEntitlementResponse;
        try {
            EmptyRpoRequest emptyRpoRequest = EmptyRpoRequest.builder().build();
            profileEntitlementResponse = armRpoClient.getProfileEntitlementResponse(bearerToken, emptyRpoRequest);
        } catch (FeignException e) {
            throw handleFailureAndCreateException(exceptionMessageBuilder.append("API call failed: ")
                                                      .append(e)
                                                      .toString(),
                                                  executionDetail, userAccount);
        }
        log.debug("ARM RPO Response - ProfileEntitlementResponse: {}", profileEntitlementResponse);
        processGetProfileEntitlementsResponse(userAccount, profileEntitlementResponse, exceptionMessageBuilder, executionDetail);
    }

    private void processGetProfileEntitlementsResponse(UserAccountEntity userAccount, ProfileEntitlementResponse profileEntitlementResponse,
                                                       StringBuilder exceptionMessageBuilder,
                                                       ArmRpoExecutionDetailEntity executionDetail) {
        handleResponseStatus(userAccount, profileEntitlementResponse, exceptionMessageBuilder, executionDetail);

        var entitlements = profileEntitlementResponse.getEntitlements();
        if (CollectionUtils.isEmpty(entitlements)) {
            throw handleFailureAndCreateException(exceptionMessageBuilder.append("No entitlements were returned").toString(),
                                                  executionDetail, userAccount);
        }

        String configuredEntitlement = armApiConfigurationProperties.getArmServiceEntitlement();
        var profileEntitlement = entitlements.stream()
            .filter(entitlement -> configuredEntitlement.equals(entitlement.getName()))
            .findFirst()
            .orElseThrow(() -> handleFailureAndCreateException(
                exceptionMessageBuilder.append("No matching entitlements '").append(configuredEntitlement).append("' were returned").toString(),
                executionDetail, userAccount));

        String entitlementId = profileEntitlement.getEntitlementId();
        if (StringUtils.isEmpty(entitlementId)) {
            throw handleFailureAndCreateException(exceptionMessageBuilder.append("The obtained entitlement id was empty").toString(),
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
        log.debug("getMasterIndexFieldByRecordClassSchema called with executionId: {}, rpo state: {}", executionId, rpoStateEntity.getId());
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, rpoStateEntity,
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);

        StringBuilder errorMessage = new StringBuilder(273).append("Failure during ARM get master index field by record class schema: ");

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
            log.error(errorMessage.append(UNABLE_TO_GET_ARM_RPO_RESPONSE).append(e).toString(), e);
            throw handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
        }
        log.debug("ARM RPO Response - MasterIndexFieldByRecordClassSchemaResponse: {}", masterIndexFieldByRecordClassSchemaResponse);
        return processMasterIndexFieldByRecordClassSchemas(userAccount, masterIndexFieldByRecordClassSchemaResponse, errorMessage, armRpoExecutionDetailEntity);
    }

    private List<MasterIndexFieldByRecordClassSchema> processMasterIndexFieldByRecordClassSchemas(
        UserAccountEntity userAccount, MasterIndexFieldByRecordClassSchemaResponse masterIndexFieldByRecordClassSchemaResponse,
        StringBuilder errorMessage, ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {

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

        log.debug("addAsyncSearch called with executionId: {}", executionId);
        final ArmRpoExecutionDetailEntity executionDetail = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(executionDetail,
                                                 ArmRpoHelper.addAsyncSearchRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(),
                                                 userAccount);

        OffsetDateTime now = currentTimeHelper.currentOffsetDateTime();
        String searchName = "DARTS_RPO_%s".formatted(
            now.format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss"))
        );

        final StringBuilder exceptionMessageBuilder = new StringBuilder(99).append("ARM addAsyncSearch: ");
        ArmAutomatedTaskEntity armAutomatedTaskEntity = armAutomatedTaskRepository.findByAutomatedTask_taskName(ADD_ASYNC_SEARCH_RELATED_TASK_NAME)
            .orElseThrow(() -> handleFailureAndCreateException(exceptionMessageBuilder.append("Automated task not found: ")
                                                                   .append(ADD_ASYNC_SEARCH_RELATED_TASK_NAME)
                                                                   .toString(),
                                                               executionDetail, userAccount));

        AddAsyncSearchRequestGenerator requestGenerator;
        try {
            requestGenerator = createAddAsyncSearchRequestGenerator(searchName, executionDetail, armAutomatedTaskEntity, now);
        } catch (Exception e) {
            throw handleFailureAndCreateException(exceptionMessageBuilder.append(COULD_NOT_CONSTRUCT_API_REQUEST)
                                                      .append(e)
                                                      .toString(),
                                                  executionDetail, userAccount);
        }

        ArmAsyncSearchResponse armAsyncSearchResponse;
        try {
            armAsyncSearchResponse = armRpoClient.addAsyncSearch(bearerToken, requestGenerator.getJsonRequest());
        } catch (FeignException e) {
            throw handleFailureAndCreateException(exceptionMessageBuilder.append("API call failed: ").append(e).toString(),
                                                  executionDetail, userAccount);
        }
        log.debug("ARM RPO Response - ArmAsyncSearchResponse: {}", armAsyncSearchResponse);
        return processAddAsyncSearch(userAccount, armAsyncSearchResponse, exceptionMessageBuilder, executionDetail, searchName);
    }

    private String processAddAsyncSearch(UserAccountEntity userAccount, ArmAsyncSearchResponse armAsyncSearchResponse, StringBuilder exceptionMessageBuilder,
                                         ArmRpoExecutionDetailEntity executionDetail, String searchName) {
        handleResponseStatus(userAccount, armAsyncSearchResponse, exceptionMessageBuilder, executionDetail);

        String searchId = armAsyncSearchResponse.getSearchId();
        if (searchId == null) {
            throw handleFailureAndCreateException(exceptionMessageBuilder.append("The obtained search id was empty").toString(),
                                                  executionDetail, userAccount);
        }

        executionDetail.setSearchId(searchId);
        armRpoService.updateArmRpoStatus(executionDetail, ArmRpoHelper.completedRpoStatus(), userAccount);

        return searchName;
    }


    @Override
    public void saveBackgroundSearch(String bearerToken, Integer executionId, String searchName, UserAccountEntity userAccount) {
        log.debug("saveBackgroundSearch called with executionId: {}, searchName: {}", executionId, searchName);
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, ArmRpoHelper.saveBackgroundSearchRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);

        StringBuilder errorMessage = new StringBuilder(134).append("Failure during ARM save background search: ");
        SaveBackgroundSearchResponse saveBackgroundSearchResponse;
        try {
            SaveBackgroundSearchRequest saveBackgroundSearchRequest =
                createSaveBackgroundSearchRequest(searchName, armRpoExecutionDetailEntity.getSearchId());
            saveBackgroundSearchResponse = armRpoClient.saveBackgroundSearch(bearerToken, saveBackgroundSearchRequest);
        } catch (FeignException e) {
            log.error(errorMessage.append("Unable to save background search").append(e).toString(), e);
            throw handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
        }

        handleResponseStatus(userAccount, saveBackgroundSearchResponse, errorMessage, armRpoExecutionDetailEntity);

        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.completedRpoStatus(), userAccount);
    }

    @Override
    public String getExtendedSearchesByMatter(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        log.debug("getExtendedSearchesByMatter called with executionId: {}", executionId);
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, ArmRpoHelper.getExtendedSearchesByMatterRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);

        StringBuilder errorMessage = new StringBuilder(153).append("Failure during ARM RPO getExtendedSearchesByMatter: ");
        GetExtendedSearchesByMatterRequestGenerator requestGenerator;
        try {
            requestGenerator = createExtendedSearchesByMatterRequestGenerator(armRpoExecutionDetailEntity.getMatterId());
        } catch (Exception e) {
            throw handleFailureAndCreateException(errorMessage.append(COULD_NOT_CONSTRUCT_API_REQUEST).append(e)
                                                      .toString(), armRpoExecutionDetailEntity, userAccount);
        }

        ExtendedSearchesByMatterResponse extendedSearchesByMatterResponse;
        try {
            extendedSearchesByMatterResponse = armRpoClient.getExtendedSearchesByMatter(bearerToken, requestGenerator.getJsonRequest());
        } catch (FeignException e) {
            log.error(errorMessage.append("Unable to get ARM RPO response {}").append(e).toString(), e);
            throw handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
        }
        log.debug("ARM RPO Response - ExtendedSearchesByMatterResponse: {}", extendedSearchesByMatterResponse);
        return processExtendedSearchesByMatterResponse(executionId, userAccount, extendedSearchesByMatterResponse, errorMessage, armRpoExecutionDetailEntity);
    }

    private String processExtendedSearchesByMatterResponse(Integer executionId, UserAccountEntity userAccount,
                                                           ExtendedSearchesByMatterResponse extendedSearchesByMatterResponse,
                                                           StringBuilder errorMessage, ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
        handleResponseStatus(userAccount, extendedSearchesByMatterResponse, errorMessage, armRpoExecutionDetailEntity);

        if (isNull(extendedSearchesByMatterResponse.getSearches())
            || CollectionUtils.isEmpty(extendedSearchesByMatterResponse.getSearches())) {

            throw handleFailureAndCreateException(errorMessage.append("Search data is missing").toString(),
                                                  armRpoExecutionDetailEntity, userAccount);
        }

        String searchId = armRpoExecutionDetailEntity.getSearchId();

        ExtendedSearchesByMatterResponse.SearchDetail searchDetailMatch = null;
        for (ExtendedSearchesByMatterResponse.SearchDetail searchDetail : extendedSearchesByMatterResponse.getSearches()) {
            if (nonNull(searchDetail.getSearch())
                && !StringUtils.isBlank(searchDetail.getSearch().getSearchId())
                && searchDetail.getSearch().getSearchId().equals(searchId)) {
                searchDetailMatch = searchDetail;
                break;
            }
        }
        if (isNull(searchDetailMatch)
            || isNull(searchDetailMatch.getSearch().getTotalCount())
            || StringUtils.isBlank(searchDetailMatch.getSearch().getName())
            || isNull(searchDetailMatch.getSearch().getIsSaved())) {
            throw handleFailureAndCreateException(errorMessage.append("extendedSearchesByMatterResponse search data is missing for searchId: ")
                                                      .append(searchId).append(" (total_count, name, is_saved) ").append(searchDetailMatch).toString(),
                                                  armRpoExecutionDetailEntity,
                                                  userAccount);
        }

        if (FALSE.equals(searchDetailMatch.getSearch().getIsSaved())) {
            log.warn(errorMessage.append("The extendedSearchesByMatterResponse is_saved attribute is FALSE for executionId: ").append(executionId).toString());
            throw new ArmRpoInProgressException("extendedSearchesByMatterResponse", executionId);
        }
        armRpoExecutionDetailEntity.setSearchItemCount(searchDetailMatch.getSearch().getTotalCount());
        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.completedRpoStatus(), userAccount);
        return searchDetailMatch.getSearch().getName();
    }

    @Override
    public boolean createExportBasedOnSearchResultsTable(String bearerToken, Integer executionId,
                                                         List<MasterIndexFieldByRecordClassSchema> headerColumns,
                                                         String uniqueProductionName, Duration pollDuration,
                                                         UserAccountEntity userAccount) {

        log.debug("createExportBasedOnSearchResultsTable called with executionId: {}, uniqueProductionName: {}", executionId, uniqueProductionName);
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        if (isNull(armRpoExecutionDetailEntity.getPollingCreatedTs())) {
            armRpoExecutionDetailEntity.setPollingCreatedTs(currentTimeHelper.currentOffsetDateTime());
        }
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, ArmRpoHelper.createExportBasedOnSearchResultsTableRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);

        StringBuilder errorMessage = new StringBuilder("Failure during ARM createExportBasedOnSearchResultsTable: ");
        CreateExportBasedOnSearchResultsTableRequest request;
        try {
            request = createRequestForCreateExportBasedOnSearchResultsTable(
                headerColumns, armRpoExecutionDetailEntity.getSearchId(),
                armRpoExecutionDetailEntity.getSearchItemCount(),
                uniqueProductionName,
                armRpoExecutionDetailEntity.getStorageAccountId()
            );
        } catch (Exception e) {
            throw handleFailureAndCreateException(errorMessage.append(COULD_NOT_CONSTRUCT_API_REQUEST).append(e).toString(),
                                                  armRpoExecutionDetailEntity, userAccount);
        }
        BaseRpoResponse baseRpoResponse;
        try {
            baseRpoResponse = armRpoClient.createExportBasedOnSearchResultsTable(bearerToken, request);
        } catch (FeignException feignException) {
            baseRpoResponse = processCreateExportBasedOnSearchResultsTableResponseFeignException(userAccount, feignException, errorMessage,
                                                                                                 armRpoExecutionDetailEntity);
        }
        log.debug("ARM RPO Response - CreateExportBasedOnSearchResultsTable response: {}", baseRpoResponse);
        return processCreateExportBasedOnSearchResultsTableResponse(userAccount, baseRpoResponse, errorMessage,
                                                                    armRpoExecutionDetailEntity, pollDuration, uniqueProductionName);
    }


    private BaseRpoResponse processCreateExportBasedOnSearchResultsTableResponseFeignException(
        UserAccountEntity userAccount, FeignException feignException, StringBuilder errorMessage, ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
        BaseRpoResponse baseRpoResponse;
        baseRpoResponse = getBaseRpoResponse(userAccount, feignException, errorMessage, armRpoExecutionDetailEntity);
        return baseRpoResponse;
    }

    private BaseRpoResponse getBaseRpoResponse(UserAccountEntity userAccount, FeignException feignException, StringBuilder errorMessage,
                                               ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
        String feignResponse = feignException.contentUTF8();
        if (StringUtils.isEmpty(feignResponse)) {
            throw handleFailureAndCreateException(errorMessage.append(UNABLE_TO_GET_ARM_RPO_RESPONSE).append(feignException).toString(),
                                                  armRpoExecutionDetailEntity, userAccount);
        }
        log.debug("ARM RPO Response - Feign response: {}", feignResponse);
        BaseRpoResponse baseRpoResponse;
        try {
            baseRpoResponse = objectMapper.readValue(feignResponse, BaseRpoResponse.class);
        } catch (JsonProcessingException ex) {
            log.warn("Unable to parse feign response: {}", feignResponse, ex);
            throw handleFailureAndCreateException(errorMessage.append(UNABLE_TO_GET_ARM_RPO_RESPONSE).append(feignException).toString(),
                                                  armRpoExecutionDetailEntity, userAccount);
        }
        return baseRpoResponse;
    }

    private boolean processCreateExportBasedOnSearchResultsTableResponse(UserAccountEntity userAccount,
                                                                         BaseRpoResponse baseRpoResponse,
                                                                         StringBuilder errorMessage,
                                                                         ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity,
                                                                         Duration pollDuration, String uniqueProductionName) {

        if (isNull(baseRpoResponse) || isNull(baseRpoResponse.getStatus()) || isNull(baseRpoResponse.getIsError())
            || (!baseRpoResponse.getIsError() && isNull(baseRpoResponse.getResponseStatus()))
        ) {
            throw handleFailureAndCreateException(errorMessage.append("ARM RPO API createExportBasedOnSearchResultsTable is invalid - ")
                                                      .append(baseRpoResponse).toString(),
                                                  armRpoExecutionDetailEntity, userAccount);
        }
        try {
            HttpStatus httpStatus = HttpStatus.valueOf(baseRpoResponse.getStatus());

            if (HttpStatus.BAD_REQUEST.value() == httpStatus.value()) {
                if (baseRpoResponse.getResponseStatus() == CREATE_EXPORT_BASED_ON_SEARCH_RESULTS_IN_PROGRESS_STATUS) {
                    return checkCreateExportBasedOnSearchResultsInProgress(userAccount, baseRpoResponse, errorMessage,
                                                                           armRpoExecutionDetailEntity, pollDuration);
                } else {
                    throw handleFailureAndCreateException(errorMessage.append("ARM RPO API failed with invalid status - ").append(httpStatus)
                                                              .append(AND_RESPONSE).append(
                                                                  baseRpoResponse).toString(),
                                                          armRpoExecutionDetailEntity, userAccount);
                }
            } else if (!httpStatus.is2xxSuccessful() || TRUE.equals(baseRpoResponse.getIsError())) {
                throw handleFailureAndCreateException(errorMessage.append("ARM RPO API failed with status - ").append(httpStatus)
                                                          .append(AND_RESPONSE).append(baseRpoResponse).toString(),
                                                      armRpoExecutionDetailEntity, userAccount);
            }
        } catch (IllegalArgumentException e) {
            throw handleFailureAndCreateException(errorMessage.append("ARM RPO API baseRpoResponse status is invalid - ")
                                                      .append(baseRpoResponse).toString(),
                                                  armRpoExecutionDetailEntity, userAccount);
        }
        armRpoExecutionDetailEntity.setProductionName(uniqueProductionName);
        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.completedRpoStatus(), userAccount);
        return true;
    }

    boolean checkCreateExportBasedOnSearchResultsInProgress(UserAccountEntity userAccount,
                                                            BaseRpoResponse baseRpoResponse,
                                                            StringBuilder errorMessage, ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity,
                                                            Duration pollDuration) {
        if (isNull(armRpoExecutionDetailEntity.getPollingCreatedTs())) {
            log.error("checkCreateExportBasedOnSearchResults is still In-Progress - {}", baseRpoResponse);
            return false;
        } else if (Duration.between(armRpoExecutionDetailEntity.getPollingCreatedTs(),
                                    currentTimeHelper.currentOffsetDateTime())
            .compareTo(pollDuration) <= 0) {
            log.error("The search is still running and cannot export as csv - {}", baseRpoResponse);
            return false;
        } else {
            throw handleFailureAndCreateException(errorMessage.append("Polling can only run for a maximum of ").append(pollDuration).toString(),
                                                  armRpoExecutionDetailEntity, userAccount);
        }
    }

    @Override
    public boolean getExtendedProductionsByMatter(String bearerToken, Integer executionId, String uniqueProductionName, UserAccountEntity userAccount) {
        log.debug("getExtendedProductionsByMatter called with executionId: {}, uniqueProductionName: {}", executionId, uniqueProductionName);
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, ArmRpoHelper.getExtendedProductionsByMatterRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);

        StringBuilder errorMessage = new StringBuilder("Failure during ARM RPO Extended Productions By Matter: ");

        GetExtendedProductionsByMatterRequestGenerator requestGenerator;
        try {
            requestGenerator = createExtendedProductionsByMatterRequest(armRpoExecutionDetailEntity.getMatterId());
        } catch (Exception e) {
            throw handleFailureAndCreateException(errorMessage.append(COULD_NOT_CONSTRUCT_API_REQUEST).append(e)
                                                      .toString(), armRpoExecutionDetailEntity, userAccount);
        }

        ExtendedProductionsByMatterResponse extendedProductionsByMatterResponse;
        try {
            extendedProductionsByMatterResponse = armRpoClient.getExtendedProductionsByMatter(bearerToken, requestGenerator.getJsonRequest());
        } catch (FeignException e) {
            log.error(errorMessage.append(UNABLE_TO_GET_ARM_RPO_RESPONSE).append(e).toString(), e);
            throw handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
        }
        log.debug("ARM RPO Response - ExtendedProductionsByMatterResponse: {}", extendedProductionsByMatterResponse);
        return processExtendedProductionsByMatterResponse(uniqueProductionName, userAccount, extendedProductionsByMatterResponse, errorMessage,
                                                          armRpoExecutionDetailEntity);
    }

    private boolean processExtendedProductionsByMatterResponse(String productionName, UserAccountEntity userAccount,
                                                               ExtendedProductionsByMatterResponse extendedProductionsByMatterResponse,
                                                               StringBuilder errorMessage, ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
        handleResponseStatus(userAccount, extendedProductionsByMatterResponse, errorMessage, armRpoExecutionDetailEntity);
        if (isNull(extendedProductionsByMatterResponse.getProductions())
            || CollectionUtils.isEmpty(extendedProductionsByMatterResponse.getProductions())) {
            throw handleFailureAndCreateException(errorMessage.append("ProductionId is missing from ARM RPO response").toString(),
                                                  armRpoExecutionDetailEntity, userAccount);
        }

        ExtendedProductionsByMatterResponse.Productions productionMatch = null;
        for (ExtendedProductionsByMatterResponse.Productions productionsItem : extendedProductionsByMatterResponse.getProductions()) {
            if (nonNull(productionsItem) && !StringUtils.isBlank(productionsItem.getName()) && productionName.equals(productionsItem.getName())) {
                productionMatch = productionsItem;
                break;
            }
        }

        if (isNull(productionMatch)) {
            log.warn(errorMessage.append("No production id found against the production name: ")
                         .append(productionName).append(", so continue polling").toString());
            return false;
        }
        if (StringUtils.isBlank(productionMatch.getProductionId())) {
            throw handleFailureAndCreateException(errorMessage.append("Production Id is missing from ARM RPO response").toString(),
                                                  armRpoExecutionDetailEntity, userAccount);
        }

        if (isNull(productionMatch.getEndProductionTime())) {
            log.warn(errorMessage.append("End production time is missing from ARM RPO response, so continue polling").toString());
            return false;
        }

        armRpoExecutionDetailEntity.setProductionId(productionMatch.getProductionId());
        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.completedRpoStatus(), userAccount);
        return true;
    }

    @Override
    public List<String> getProductionOutputFiles(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        log.debug("getProductionOutputFiles called with executionId: {}", executionId);
        final ArmRpoExecutionDetailEntity executionDetail = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(executionDetail,
                                                 ArmRpoHelper.getProductionOutputFilesRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(),
                                                 userAccount);

        final StringBuilder exceptionMessageBuilder = new StringBuilder(135).append("ARM getProductionOutputFiles: ");

        String productionId = executionDetail.getProductionId();
        if (StringUtils.isBlank(productionId)) {
            throw handleFailureAndCreateException(exceptionMessageBuilder.append("production id was blank for execution id: ")
                                                      .append(executionId).toString(),
                                                  executionDetail, userAccount);
        }

        ProductionOutputFilesResponse productionOutputFilesResponse;
        try {
            productionOutputFilesResponse = armRpoClient.getProductionOutputFiles(bearerToken, createProductionOutputFilesRequest(productionId));
        } catch (FeignException e) {
            throw handleFailureAndCreateException(exceptionMessageBuilder.append("API call failed: ").append(e).toString(),
                                                  executionDetail, userAccount);
        }
        log.debug("ARM RPO Response - ProductionOutputFilesResponse: {}", productionOutputFilesResponse);
        return processProductionOutputFilesResponse(userAccount, productionOutputFilesResponse, exceptionMessageBuilder, executionDetail);
    }

    private List<String> processProductionOutputFilesResponse(UserAccountEntity userAccount, ProductionOutputFilesResponse productionOutputFilesResponse,
                                                              StringBuilder exceptionMessageBuilder,
                                                              ArmRpoExecutionDetailEntity executionDetail) {
        handleResponseStatus(userAccount, productionOutputFilesResponse, exceptionMessageBuilder, executionDetail);

        List<ProductionOutputFilesResponse.ProductionExportFile> productionExportFiles = productionOutputFilesResponse.getProductionExportFiles();
        if (CollectionUtils.isEmpty(productionExportFiles)
            || productionExportFiles.stream().anyMatch(Objects::isNull)) {
            throw handleFailureAndCreateException(exceptionMessageBuilder.append("No production export files were returned").toString(),
                                                  executionDetail, userAccount);
        }

        validateProductionExportResponse(userAccount, exceptionMessageBuilder, executionDetail, productionExportFiles);

        List<String> productionExportFileIds = productionExportFiles.stream()
            .filter(Objects::nonNull)
            .map(ProductionOutputFilesResponse.ProductionExportFile::getProductionExportFileDetails)
            .filter(Objects::nonNull)
            .filter(productionExportFileDetails -> productionExportFileDetails.getStatus() == READY_STATUS.getStatusCode())
            .map(ProductionOutputFilesResponse.ProductionExportFileDetail::getProductionExportFileId)
            .filter(StringUtils::isNotBlank)
            .toList();

        if (productionExportFileIds.isEmpty()) {
            throw handleFailureAndCreateException(exceptionMessageBuilder.append("No production export file id's were returned").toString(),
                                                  executionDetail, userAccount);
        }

        armRpoService.updateArmRpoStatus(executionDetail, ArmRpoHelper.completedRpoStatus(), userAccount);

        return productionExportFileIds;
    }

    private void validateProductionExportResponse(UserAccountEntity userAccount, StringBuilder exceptionMessageBuilder,
                                                  ArmRpoExecutionDetailEntity executionDetail,
                                                  List<ProductionOutputFilesResponse.ProductionExportFile> productionExportFiles) {
        if (productionExportFiles.stream().map(ProductionOutputFilesResponse.ProductionExportFile::getProductionExportFileDetails)
            .anyMatch(details -> nonNull(details) && IN_PROGRESS_STATUS.getStatusCode() == details.getStatus())) {
            throw new ArmRpoInProgressException("getProductionExportFileDetails", executionDetail.getId());
        } else if (productionExportFiles.stream().map(ProductionOutputFilesResponse.ProductionExportFile::getProductionExportFileDetails)
            .allMatch(details -> nonNull(details) && READY_STATUS.getStatusCode() == details.getStatus())) {
            log.info("All production export files are ready for download");
        } else if (productionExportFiles.stream().map(ProductionOutputFilesResponse.ProductionExportFile::getProductionExportFileDetails)
            .allMatch(details -> nonNull(details) && details.getStatus() != READY_STATUS.getStatusCode())) {
            throw handleFailureAndCreateException(exceptionMessageBuilder.append("Production export files contain failures").toString(),
                                                  executionDetail, userAccount);
        } else if (productionExportFiles.stream().map(ProductionOutputFilesResponse.ProductionExportFile::getProductionExportFileDetails)
            .anyMatch(details -> nonNull(details) && READY_STATUS.getStatusCode() == details.getStatus())) {
            log.warn("Some production export files are not ready for download");
        } else {
            throw handleFailureAndCreateException(exceptionMessageBuilder.append("Production export files contain failures").toString(),
                                                  executionDetail, userAccount);
        }
    }

    @Override
    public InputStream downloadProduction(String bearerToken, Integer executionId, String productionExportFileId,
                                          UserAccountEntity userAccount) throws IOException {
        log.debug("downloadProduction called with executionId: {}, productionExportFileId: {}", executionId, productionExportFileId);
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, ArmRpoHelper.downloadProductionRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);

        feign.Response response;
        StringBuilder errorMessage = new StringBuilder(185).append("Failure during download production: ");

        try {
            response = armRpoDownloadProduction.downloadProduction(bearerToken, executionId, productionExportFileId);
        } catch (FeignException e) {
            log.error(errorMessage.append("Error during ARM RPO download production id: ").append(productionExportFileId)
                          .append(e).toString(), e);
            throw handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
        }
        log.debug("ARM RPO Response - downloadProduction response: {}", response);

        // on any error occurring, return a download failure
        if (isNull(response) || !HttpStatus.valueOf(response.status()).is2xxSuccessful()) {
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
        log.debug("removeProduction called with executionId: {}", executionId);
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, ArmRpoHelper.removeProductionRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);

        StringBuilder errorMessage = new StringBuilder("Failure during ARM RPO removeProduction: ");
        RemoveProductionResponse removeProductionResponse;
        try {
            RemoveProductionRequest request = createRemoveProductionRequest(armRpoExecutionDetailEntity);
            removeProductionResponse = armRpoClient.removeProduction(bearerToken, request);
        } catch (FeignException e) {
            // this ensures the full error body containing the ARM error detail is logged rather than a truncated version
            log.error(errorMessage.append(UNABLE_TO_GET_ARM_RPO_RESPONSE).append(e).toString(), e);
            throw handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
        }
        log.debug("ARM RPO Response - removeProduction response: {}", removeProductionResponse);
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

    private IndexesByMatterIdRequest createIndexesByMatterIdRequest(String matterId) {
        return IndexesByMatterIdRequest.builder()
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
            throw handleFailureAndCreateException(errorMessage.append("ARM RPO API response is invalid - ").append(baseRpoResponse).toString(),
                                                  armRpoExecutionDetailEntity, userAccount);
        }
        try {
            HttpStatus responseStatus = HttpStatus.valueOf(baseRpoResponse.getStatus());
            if (!responseStatus.is2xxSuccessful() || baseRpoResponse.getIsError()) {
                throw handleFailureAndCreateException(errorMessage.append("ARM RPO API failed with status - ").append(responseStatus)
                                                          .append(AND_RESPONSE).append(baseRpoResponse).toString(),
                                                      armRpoExecutionDetailEntity, userAccount);
            }
        } catch (IllegalArgumentException e) {
            log.error(errorMessage.append("ARM RPO API response status is invalid - ").append(baseRpoResponse).toString(), e);
            throw handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
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
                                                                                OffsetDateTime now) {
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
            .onlyForCurrentUser(FALSE)
            .exportType(32)
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
