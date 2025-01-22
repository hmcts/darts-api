package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.EmptyRpoRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.RecordManagementMatterResponse;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.rpo.GetRecordManagementMatterService;
import uk.gov.hmcts.darts.arm.service.ArmRpoApiHelper;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import static java.util.Objects.isNull;

@Service
@AllArgsConstructor
@Slf4j
public class GetRecordManagementMatterServiceImpl implements GetRecordManagementMatterService {

    private final ArmRpoService armRpoService;
    private final ArmRpoClient armRpoClient;
    private final ArmRpoApiHelper armRpoApiHelper;

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
            log.error(errorMessage.append("Unable to get ARM RPO response").append(e).toString(), e);
            throw armRpoApiHelper.handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
        }

        armRpoApiHelper.handleResponseStatus(userAccount, recordManagementMatterResponse, errorMessage, armRpoExecutionDetailEntity);

        if (isNull(recordManagementMatterResponse.getRecordManagementMatter())
            || StringUtils.isBlank(recordManagementMatterResponse.getRecordManagementMatter().getMatterId())) {
            throw armRpoApiHelper.handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
        }

        armRpoExecutionDetailEntity.setMatterId(recordManagementMatterResponse.getRecordManagementMatter().getMatterId());
        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.completedRpoStatus(), userAccount);
    }
}
