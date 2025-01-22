package uk.gov.hmcts.darts.arm.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.client.model.rpo.BaseRpoResponse;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.service.ArmRpoApiHelper;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

@Service
@AllArgsConstructor
public class ArmRpoApiHelperImpl implements ArmRpoApiHelper {

    private final ArmRpoService armRpoService;

    public void handleResponseStatus(UserAccountEntity userAccount, BaseRpoResponse baseRpoResponse, StringBuilder errorMessage,
                                     ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
        if (baseRpoResponse == null || baseRpoResponse.getStatus() == null || baseRpoResponse.getIsError() == null) {
            throw handleFailureAndCreateException(errorMessage.append("ARM RPO API response is invalid - ").append(baseRpoResponse).toString(),
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

    public ArmRpoException handleFailureAndCreateException(String message, ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity,
                                                           UserAccountEntity userAccount) {
        // Assuming armRpoService is available here
        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.failedRpoStatus(), userAccount);
        return new ArmRpoException(message);
    }
}
