package uk.gov.hmcts.darts.arm.util;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.client.model.rpo.BaseRpoResponse;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.UUID;

import static java.util.Objects.isNull;

@Component
@AllArgsConstructor
@Slf4j
public class ArmRpoUtil {

    public static final String UNABLE_TO_GET_ARM_RPO_RESPONSE = "Unable to get ARM RPO response from client ";
    public static final String COULD_NOT_CONSTRUCT_API_REQUEST = "Could not construct API request: ";

    private static final String CREATE_EXPORT_CSV_EXTENSION = "_CSV";
    private static final String AND_RESPONSE = " and response - ";

    private final ArmRpoService armRpoService;

    public String generateUniqueProductionName(String productionName) {
        return productionName + "_" + UUID.randomUUID().toString() + CREATE_EXPORT_CSV_EXTENSION;
    }

    public ArmRpoException handleFailureAndCreateException(String message,
                                                           ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity,
                                                           UserAccountEntity userAccount) {
        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.failedRpoStatus(), userAccount);
        return new ArmRpoException(message);
    }

    public void handleResponseStatus(UserAccountEntity userAccount, BaseRpoResponse baseRpoResponse, StringBuilder errorMessage,
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
}
