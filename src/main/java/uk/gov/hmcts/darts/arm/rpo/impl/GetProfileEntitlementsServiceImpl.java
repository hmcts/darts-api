package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.EmptyRpoRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.ProfileEntitlementResponse;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.rpo.GetProfileEntitlementsService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

@Service
@AllArgsConstructor
@Slf4j
public class GetProfileEntitlementsServiceImpl implements GetProfileEntitlementsService {

    private final ArmRpoClient armRpoClient;
    private final ArmRpoService armRpoService;
    private final ArmRpoUtil armRpoUtil;
    private final ArmApiConfigurationProperties armApiConfigurationProperties;

    @Override
    public void getProfileEntitlements(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        log.info("getProfileEntitlements called with executionId: {}", executionId);
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
            throw armRpoUtil.handleFailureAndCreateException(exceptionMessageBuilder.append("API call failed: ")
                                                                 .append(e)
                                                                 .toString(),
                                                             executionDetail, userAccount, e);
        }
        log.info("ARM RPO Response - ProfileEntitlementResponse: {}", profileEntitlementResponse);
        processGetProfileEntitlementsResponse(userAccount, profileEntitlementResponse, exceptionMessageBuilder, executionDetail);
    }

    private void processGetProfileEntitlementsResponse(UserAccountEntity userAccount, ProfileEntitlementResponse profileEntitlementResponse,
                                                       StringBuilder exceptionMessageBuilder,
                                                       ArmRpoExecutionDetailEntity executionDetail) {
        armRpoUtil.handleResponseStatus(userAccount, profileEntitlementResponse, exceptionMessageBuilder, executionDetail);

        var entitlements = profileEntitlementResponse.getEntitlements();
        if (CollectionUtils.isEmpty(entitlements)) {
            throw armRpoUtil.handleFailureAndCreateException(exceptionMessageBuilder.append("No entitlements were returned").toString(),
                                                             executionDetail, userAccount);
        }

        String configuredEntitlement = armApiConfigurationProperties.getArmServiceEntitlement();
        var profileEntitlement = entitlements.stream()
            .filter(entitlement -> configuredEntitlement.equals(entitlement.getName()))
            .findFirst()
            .orElseThrow(() -> armRpoUtil.handleFailureAndCreateException(
                exceptionMessageBuilder.append("No matching entitlements '").append(configuredEntitlement).append("' were returned").toString(),
                executionDetail, userAccount));

        String entitlementId = profileEntitlement.getEntitlementId();
        if (StringUtils.isEmpty(entitlementId)) {
            throw armRpoUtil.handleFailureAndCreateException(exceptionMessageBuilder.append("The obtained entitlement id was empty").toString(),
                                                             executionDetail, userAccount);
        }

        executionDetail.setEntitlementId(entitlementId);
        armRpoService.updateArmRpoStatus(executionDetail, ArmRpoHelper.completedRpoStatus(), userAccount);
    }
}
