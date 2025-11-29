package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.client.model.rpo.StorageAccountRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.StorageAccountResponse;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.rpo.GetStorageAccountsService;
import uk.gov.hmcts.darts.arm.service.ArmClientService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
@SuppressWarnings({"PMD.PreserveStackTrace"})
public class GetStorageAccountsServiceImpl implements GetStorageAccountsService {

    private final ArmClientService armClientService;
    private final ArmRpoService armRpoService;
    private final ArmRpoUtil armRpoUtil;
    private final ArmApiConfigurationProperties armApiConfigurationProperties;

    @Override
    public void getStorageAccounts(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        log.info("getStorageAccounts called with executionId: {}", executionId);
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, ArmRpoHelper.getStorageAccountsRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);
        StringBuilder errorMessage = new StringBuilder(70);
        errorMessage.append("Failure during ARM get storage accounts: ");
        StorageAccountResponse storageAccountResponse;
        StorageAccountRequest storageAccountRequest = createStorageAccountRequest();
        try {
            storageAccountResponse = armClientService.getStorageAccounts(bearerToken, storageAccountRequest);
        } catch (FeignException feignException) {
            log.error(errorMessage.append(ArmRpoUtil.UNABLE_TO_GET_ARM_RPO_RESPONSE).append(feignException.getMessage()).toString(), feignException);
            int status = feignException.status();
            // If unauthorized or forbidden, retry once with a refreshed token
            if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
                try {
                    String refreshedBearer = armRpoUtil.retryGetBearerToken("getStorageAccounts");
                    storageAccountResponse = armClientService.getStorageAccounts(refreshedBearer, storageAccountRequest);
                } catch (FeignException retryEx) {
                    throw armRpoUtil.handleFailureAndCreateException(
                        errorMessage.append("API call failed after retry: ").append(retryEx.getMessage()).toString(),
                        armRpoExecutionDetailEntity, userAccount);
                }
            } else {
                throw armRpoUtil.handleFailureAndCreateException(errorMessage.append("API call failed: ").append(feignException.getMessage()).toString(),
                                                                 armRpoExecutionDetailEntity, userAccount);
            }
        }
        log.info("ARM RPO Response - StorageAccountResponse: {}", storageAccountResponse);
        processGetStorageAccountsResponse(userAccount, storageAccountResponse, errorMessage, armRpoExecutionDetailEntity);
    }

    private void processGetStorageAccountsResponse(UserAccountEntity userAccount, StorageAccountResponse storageAccountResponse, StringBuilder errorMessage,
                                                   ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
        armRpoUtil.handleResponseStatus(userAccount, storageAccountResponse, errorMessage, armRpoExecutionDetailEntity);

        if (!CollectionUtils.isNotEmpty(storageAccountResponse.getDataDetails())) {
            throw armRpoUtil.handleFailureAndCreateException(errorMessage.append("No data details were present in the storage account response").toString(),
                                                             armRpoExecutionDetailEntity, userAccount);
        }

        final String armStorageAccountName = armApiConfigurationProperties.getArmStorageAccountName();
        List<String> storageAccountIds = storageAccountResponse.getDataDetails().stream()
            .filter(dataDetails -> armStorageAccountName.equals(dataDetails.getName()))
            .map(StorageAccountResponse.DataDetails::getId)
            .filter(id -> !StringUtils.isBlank(id))
            .toList();

        if (CollectionUtils.isEmpty(storageAccountIds)) {
            throw armRpoUtil.handleFailureAndCreateException(errorMessage.append("Unable to find ARM RPO storage account in response").toString(),
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

    private StorageAccountRequest createStorageAccountRequest() {
        return StorageAccountRequest.builder()
            .onlyKeyAccessType(false)
            .storageType(1)
            .build();
    }
}
