package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.rpo.ArmRpoApi;
import uk.gov.hmcts.darts.arm.service.ArmApiService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.service.TriggerArmRpoSearchService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;

@Slf4j
@Service
@RequiredArgsConstructor
public class TriggerArmRpoSearchServiceImpl implements TriggerArmRpoSearchService {

    private final ArmRpoApi armRpoApi;
    private final ArmRpoService armRpoService;
    private final ArmApiService armApiService;
    private final UserIdentity userIdentity;

    /**
     * This method integrates various ARM RPO API calls to ultimately trigger a search. The results of that search are then processed by another automated
     * task.
     * <p/>
     * If any of the underlying ArmRpoApi methods fail, ArmRpoApi will explicitly set the arm_rpo_execution_detail status to FAILED for that
     * particular stage. So if you're thinking about adding any transactionality at the level of TriggerArmRpoSearchServiceImpl please be careful to avoid
     * unwanted arm_rpo_execution_detail rollbacks.
     */
    @Override
    public void triggerArmRpoSearch() {
        log.info("Triggering ARM RPO search flow...");

        var userAccountEntity = userIdentity.getUserAccount();

        final Integer executionId = armRpoService.createArmRpoExecutionDetailEntity(userAccountEntity)
            .getId();

        // armBearerToken may be null, but we'll let the lower level service methods deal with that by handling the resultant HTTP exception
        final String armBearerToken = armApiService.getArmBearerToken();

        armRpoApi.getRecordManagementMatter(armBearerToken,
                                            executionId,
                                            userAccountEntity);

        // We expect getRecordManagementMatter() to populate the matter id as a side effect, so refresh the entity to get the updated value
        final String matterId = armRpoService.getArmRpoExecutionDetailEntity(executionId)
            .getMatterId();
        armRpoApi.getIndexesByMatterId(armBearerToken,
                                       executionId,
                                       matterId,
                                       userAccountEntity);

        armRpoApi.getStorageAccounts(armBearerToken,
                                     executionId,
                                     userAccountEntity);

        armRpoApi.getProfileEntitlements(armBearerToken,
                                         executionId,
                                         userAccountEntity);

        armRpoApi.getMasterIndexFieldByRecordClassSchema(armBearerToken,
                                                         executionId,
                                                         ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaPrimaryRpoState(),
                                                         userAccountEntity);

        String searchName = armRpoApi.addAsyncSearch(armBearerToken,
                                                     executionId,
                                                     userAccountEntity);

        armRpoApi.saveBackgroundSearch(armBearerToken,
                                       executionId,
                                       searchName,
                                       userAccountEntity);

        log.info("ARM RPO search flow completed successfully");
    }

}
