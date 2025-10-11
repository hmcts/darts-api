package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        StringBuilder errorMessage = new StringBuilder("Failure during ARM RPO removeProduction: ");
        RemoveProductionResponse removeProductionResponse;
        try {
            RemoveProductionRequest request = createRemoveProductionRequest(armRpoExecutionDetailEntity);
            removeProductionResponse = armClientService.removeProduction(bearerToken, request);
        } catch (FeignException e) {
            // this ensures the full error body containing the ARM error detail is logged rather than a truncated version
            log.error(errorMessage.append(ArmRpoUtil.UNABLE_TO_GET_ARM_RPO_RESPONSE).append(e).toString(), e);
            throw armRpoUtil.handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
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
