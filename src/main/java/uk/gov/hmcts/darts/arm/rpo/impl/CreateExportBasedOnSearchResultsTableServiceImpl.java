package uk.gov.hmcts.darts.arm.rpo.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.BaseRpoResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.CreateExportBasedOnSearchResultsTableRequest;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.model.rpo.MasterIndexFieldByRecordClassSchema;
import uk.gov.hmcts.darts.arm.rpo.CreateExportBasedOnSearchResultsTableService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;

@Service
@AllArgsConstructor
@Slf4j
public class CreateExportBasedOnSearchResultsTableServiceImpl implements CreateExportBasedOnSearchResultsTableService {

    private static final int CREATE_EXPORT_BASED_ON_SEARCH_RESULTS_IN_PROGRESS_STATUS = 2;
    private static final String AND_RESPONSE = " and response - ";

    private final ArmRpoClient armRpoClient;
    private final ArmRpoService armRpoService;
    private final ArmRpoUtil armRpoUtil;
    private final CurrentTimeHelper currentTimeHelper;
    private final ObjectMapper objectMapper;


    @Override
    public boolean createExportBasedOnSearchResultsTable(String bearerToken, Integer executionId,
                                                         List<MasterIndexFieldByRecordClassSchema> headerColumns,
                                                         String uniqueProductionName, Duration pollDuration,
                                                         UserAccountEntity userAccount) {

        log.debug("createExportBasedOnSearchResultsTable called with executionId: {}, uniqueProductionName: {}", executionId, uniqueProductionName);
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        if (isNull(armRpoExecutionDetailEntity.getPollingCreatedAt())) {
            armRpoExecutionDetailEntity.setPollingCreatedAt(currentTimeHelper.currentOffsetDateTime());
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
            throw armRpoUtil.handleFailureAndCreateException(errorMessage.append(armRpoUtil.COULD_NOT_CONSTRUCT_API_REQUEST).append(e).toString(),
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
            throw armRpoUtil.handleFailureAndCreateException(errorMessage.append(armRpoUtil.UNABLE_TO_GET_ARM_RPO_RESPONSE).append(feignException).toString(),
                                                             armRpoExecutionDetailEntity, userAccount);
        }
        log.debug("ARM RPO Response - Feign response: {}", feignResponse);
        BaseRpoResponse baseRpoResponse;
        try {
            baseRpoResponse = objectMapper.readValue(feignResponse, BaseRpoResponse.class);
        } catch (JsonProcessingException ex) {
            log.warn("Unable to parse feign response: {}", feignResponse, ex);
            throw armRpoUtil.handleFailureAndCreateException(errorMessage.append(armRpoUtil.UNABLE_TO_GET_ARM_RPO_RESPONSE).append(feignException).toString(),
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
            throw armRpoUtil.handleFailureAndCreateException(errorMessage.append("ARM RPO API createExportBasedOnSearchResultsTable is invalid - ")
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
                    throw armRpoUtil.handleFailureAndCreateException(errorMessage.append("ARM RPO API failed with invalid status - ").append(httpStatus)
                                                                         .append(AND_RESPONSE).append(
                                                                             baseRpoResponse).toString(),
                                                                     armRpoExecutionDetailEntity, userAccount);
                }
            } else if (!httpStatus.is2xxSuccessful() || TRUE.equals(baseRpoResponse.getIsError())) {
                throw armRpoUtil.handleFailureAndCreateException(errorMessage.append("ARM RPO API failed with status - ").append(httpStatus)
                                                                     .append(AND_RESPONSE).append(baseRpoResponse).toString(),
                                                                 armRpoExecutionDetailEntity, userAccount);
            }
        } catch (IllegalArgumentException e) {
            throw armRpoUtil.handleFailureAndCreateException(errorMessage.append("ARM RPO API baseRpoResponse status is invalid - ")
                                                                 .append(baseRpoResponse).toString(),
                                                             armRpoExecutionDetailEntity, userAccount);
        }
        armRpoExecutionDetailEntity.setProductionName(uniqueProductionName);
        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.completedRpoStatus(), userAccount);
        return true;
    }

    @Override
    public boolean checkCreateExportBasedOnSearchResultsInProgress(UserAccountEntity userAccount,
                                                                   BaseRpoResponse baseRpoResponse,
                                                                   StringBuilder errorMessage, ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity,
                                                                   Duration pollDuration) {
        if (isNull(armRpoExecutionDetailEntity.getPollingCreatedAt())) {
            log.error("checkCreateExportBasedOnSearchResults is still In-Progress - {}", baseRpoResponse);
            return false;
        } else if (Duration.between(armRpoExecutionDetailEntity.getPollingCreatedAt(),
                                    currentTimeHelper.currentOffsetDateTime())
            .compareTo(pollDuration) <= 0) {
            log.error("The search is still running and cannot export as csv - {}", baseRpoResponse);
            return false;
        } else {
            throw armRpoUtil.handleFailureAndCreateException(errorMessage.append("Polling can only run for a maximum of ").append(pollDuration).toString(),
                                                             armRpoExecutionDetailEntity, userAccount);
        }
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
}
