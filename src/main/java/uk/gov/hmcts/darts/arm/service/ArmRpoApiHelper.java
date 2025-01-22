package uk.gov.hmcts.darts.arm.service;

import uk.gov.hmcts.darts.arm.client.model.rpo.BaseRpoResponse;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

public interface ArmRpoApiHelper {

    void handleResponseStatus(UserAccountEntity userAccount, BaseRpoResponse baseRpoResponse, StringBuilder errorMessage,
                              ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity);

    ArmRpoException handleFailureAndCreateException(String message, ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity,
                                                    UserAccountEntity userAccount);
}
