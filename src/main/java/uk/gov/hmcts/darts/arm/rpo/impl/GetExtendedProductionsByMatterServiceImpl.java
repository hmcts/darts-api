package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.ExtendedProductionsByMatterResponse;
import uk.gov.hmcts.darts.arm.component.impl.GetExtendedProductionsByMatterRequestGenerator;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.rpo.GetExtendedProductionsByMatterService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
@AllArgsConstructor
@Slf4j
public class GetExtendedProductionsByMatterServiceImpl implements GetExtendedProductionsByMatterService {

    private final ArmRpoClient armRpoClient;
    private final ArmRpoService armRpoService;
    private final ArmRpoUtil armRpoUtil;

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
            throw armRpoUtil.handleFailureAndCreateException(errorMessage.append(armRpoUtil.COULD_NOT_CONSTRUCT_API_REQUEST).append(e)
                                                                 .toString(), armRpoExecutionDetailEntity, userAccount);
        }

        ExtendedProductionsByMatterResponse extendedProductionsByMatterResponse;
        try {
            extendedProductionsByMatterResponse = armRpoClient.getExtendedProductionsByMatter(bearerToken, requestGenerator.getJsonRequest());
        } catch (FeignException e) {
            log.error(errorMessage.append(armRpoUtil.UNABLE_TO_GET_ARM_RPO_RESPONSE).append(e).toString(), e);
            throw armRpoUtil.handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
        }
        log.debug("ARM RPO Response - ExtendedProductionsByMatterResponse: {}", extendedProductionsByMatterResponse);
        return processExtendedProductionsByMatterResponse(uniqueProductionName, userAccount, extendedProductionsByMatterResponse, errorMessage,
                                                          armRpoExecutionDetailEntity);
    }

    private boolean processExtendedProductionsByMatterResponse(String productionName, UserAccountEntity userAccount,
                                                               ExtendedProductionsByMatterResponse extendedProductionsByMatterResponse,
                                                               StringBuilder errorMessage, ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
        armRpoUtil.handleResponseStatus(userAccount, extendedProductionsByMatterResponse, errorMessage, armRpoExecutionDetailEntity);
        if (isNull(extendedProductionsByMatterResponse.getProductions())
            || CollectionUtils.isEmpty(extendedProductionsByMatterResponse.getProductions())) {
            throw armRpoUtil.handleFailureAndCreateException(errorMessage.append("ProductionId is missing from ARM RPO response").toString(),
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
            throw armRpoUtil.handleFailureAndCreateException(errorMessage.append("Production Id is missing from ARM RPO response").toString(),
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

    private GetExtendedProductionsByMatterRequestGenerator createExtendedProductionsByMatterRequest(String matterId) {
        return GetExtendedProductionsByMatterRequestGenerator.builder()
            .matterId(matterId)
            .build();
    }

}
