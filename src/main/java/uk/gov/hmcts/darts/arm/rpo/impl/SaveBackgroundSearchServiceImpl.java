package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.SaveBackgroundSearchRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.SaveBackgroundSearchResponse;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.rpo.SaveBackgroundSearchService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

@Service
@AllArgsConstructor
@Slf4j
public class SaveBackgroundSearchServiceImpl implements SaveBackgroundSearchService {

    private final ArmRpoClient armRpoClient;
    private final ArmRpoService armRpoService;
    private final ArmRpoUtil armRpoUtil;

    @Override
    public void saveBackgroundSearch(String bearerToken, Integer executionId, String searchName, UserAccountEntity userAccount) {
        log.info("saveBackgroundSearch called with executionId: {}, searchName: {}", executionId, searchName);
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, ArmRpoHelper.saveBackgroundSearchRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);

        StringBuilder errorMessage = new StringBuilder(134).append("Failure during ARM save background search: ");
        SaveBackgroundSearchResponse saveBackgroundSearchResponse;
        try {
            SaveBackgroundSearchRequest saveBackgroundSearchRequest =
                createSaveBackgroundSearchRequest(searchName, armRpoExecutionDetailEntity.getSearchId());
            saveBackgroundSearchResponse = armRpoClient.saveBackgroundSearch(bearerToken, saveBackgroundSearchRequest);
        } catch (FeignException e) {
            log.error(errorMessage.append("Unable to save background search").append(e).toString(), e);
            throw armRpoUtil.handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
        }
        log.info("ARM RPO Response - SaveBackgroundSearchResponse: {}", saveBackgroundSearchResponse);
        armRpoUtil.handleResponseStatus(userAccount, saveBackgroundSearchResponse, errorMessage, armRpoExecutionDetailEntity);

        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.completedRpoStatus(), userAccount);
    }

    private SaveBackgroundSearchRequest createSaveBackgroundSearchRequest(String searchName, String searchId) {
        return SaveBackgroundSearchRequest.builder()
            .name(searchName)
            .searchId(searchId)
            .build();
    }

}
