package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.client.model.rpo.RemoveProductionRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.RemoveProductionResponse;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.rpo.RemoveProductionService;
import uk.gov.hmcts.darts.arm.service.ArmClientService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

@Service
@AllArgsConstructor
@Slf4j
@SuppressWarnings({"PMD.PreserveStackTrace"})
public class RemoveProductionServiceImpl implements RemoveProductionService {

    private final ArmClientService armClientService;
    private final ArmRpoService armRpoService;
    private final ArmRpoUtil armRpoUtil;

    @Override
    public void removeProduction(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        log.info("removeProduction called with executionId: {}", executionId);
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, ArmRpoHelper.removeProductionRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);

        StringBuilder errorMessage = new StringBuilder(70);
        errorMessage.append("Failure during ARM RPO removeProduction: ");
        RemoveProductionResponse removeProductionResponse;
        RemoveProductionRequest request = createRemoveProductionRequest(armRpoExecutionDetailEntity);
        try {
            removeProductionResponse = armClientService.removeProduction(bearerToken, request);
        } catch (FeignException feignException) {
            // this ensures the full error body containing the ARM error detail is logged rather than a truncated version
            log.error(errorMessage.append(ArmRpoUtil.UNABLE_TO_GET_ARM_RPO_RESPONSE).append(feignException.getMessage()).toString(), feignException);
            int status = feignException.status();
            // If unauthorized or forbidden, retry once with a refreshed token
            if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
                try {
                    String refreshedBearer = armRpoUtil.retryGetBearerToken("removeProduction");
                    removeProductionResponse = armClientService.removeProduction(refreshedBearer, request);
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
        log.info("ARM RPO Response - removeProduction response: {}", removeProductionResponse);
        armRpoUtil.handleResponseStatus(userAccount, removeProductionResponse, errorMessage, armRpoExecutionDetailEntity);

        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.completedRpoStatus(), userAccount);
    }

    private RemoveProductionRequest createRemoveProductionRequest(ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
        return RemoveProductionRequest.builder()
            .productionId(armRpoExecutionDetailEntity.getProductionId())
            .deleteSearch(true)
            .build();
    }

}
