package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.rpo.AddAsyncSearchService;
import uk.gov.hmcts.darts.arm.rpo.GetIndexesByMatterIdService;
import uk.gov.hmcts.darts.arm.rpo.GetMasterIndexFieldByRecordClassSchemaService;
import uk.gov.hmcts.darts.arm.rpo.GetProfileEntitlementsService;
import uk.gov.hmcts.darts.arm.rpo.GetRecordManagementMatterService;
import uk.gov.hmcts.darts.arm.rpo.GetStorageAccountsService;
import uk.gov.hmcts.darts.arm.rpo.SaveBackgroundSearchService;
import uk.gov.hmcts.darts.arm.service.ArmApiService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.service.TriggerArmRpoSearchService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TriggerArmRpoSearchServiceImpl implements TriggerArmRpoSearchService {

    private final ArmRpoService armRpoService;
    private final ArmApiService armApiService;
    private final UserIdentity userIdentity;
    private final LogApi logApi;
    private final GetRecordManagementMatterService getRecordManagementMatterService;
    private final GetIndexesByMatterIdService getIndexesByMatterIdService;
    private final GetStorageAccountsService getStorageAccountsService;
    private final GetProfileEntitlementsService getProfileEntitlementsService;
    private final GetMasterIndexFieldByRecordClassSchemaService getMasterIndexFieldByRecordClassSchemaService;
    private final AddAsyncSearchService addAsyncSearchService;
    private final SaveBackgroundSearchService saveBackgroundSearchService;

    /**
     * This method integrates various ARM RPO API calls to ultimately trigger a search. The results of that search are then processed by another automated
     * task.
     * <p/>
     * If any of the underlying ArmRpoApi methods fail, ArmRpoApi will explicitly set the arm_rpo_execution_detail status to FAILED for that
     * particular stage. So if you're thinking about adding any transactionality at the level of TriggerArmRpoSearchServiceImpl please be careful to avoid
     * unwanted arm_rpo_execution_detail rollbacks.
     *
     * @param threadSleepDuration the duration to sleep the thread between API call
     */
    @Override
    public void triggerArmRpoSearch(Duration threadSleepDuration) {
        log.info("Triggering ARM RPO search flow with sleep duration {}", threadSleepDuration);
        Integer executionId = null;
        try {
            var userAccountEntity = userIdentity.getUserAccount();

            executionId = armRpoService.createArmRpoExecutionDetailEntity(userAccountEntity).getId();

            // armBearerToken may be null, but we'll let the lower level service methods deal with that by handling the resultant HTTP exception
            final String armBearerToken = armApiService.getArmBearerToken();

            getRecordManagementMatterService.getRecordManagementMatter(
                armBearerToken, executionId, userAccountEntity);

            // We expect getRecordManagementMatter() to populate the matter id as a side effect, so refresh the entity to get the updated value
            final String matterId = armRpoService.getArmRpoExecutionDetailEntity(executionId).getMatterId();

            getIndexesByMatterIdService.getIndexesByMatterId(
                armBearerToken, executionId, matterId, userAccountEntity);

            getStorageAccountsService.getStorageAccounts(
                armBearerToken, executionId, userAccountEntity);

            getProfileEntitlementsService.getProfileEntitlements(
                armBearerToken, executionId, userAccountEntity);

            getMasterIndexFieldByRecordClassSchemaService.getMasterIndexFieldByRecordClassSchema(
                armBearerToken, executionId, ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaPrimaryRpoState(), userAccountEntity);

            String searchName = addAsyncSearchService.addAsyncSearch(
                armBearerToken, executionId, userAccountEntity);
            sleep(threadSleepDuration);

            saveBackgroundSearchService.saveBackgroundSearch(
                armBearerToken, executionId, searchName, userAccountEntity);

            log.info("ARM RPO search flow completed successfully");
            logApi.armRpoSearchSuccessful(executionId);
        } catch (Exception e) {
            log.error("Error occurred during ARM RPO search flow", e);
            logApi.armRpoSearchFailed(executionId);
        }
    }

    // Added method to fix sonar complaint
    void sleep(Duration threadSleepDuration) {
        try {
            Thread.sleep(threadSleepDuration);
        } catch (InterruptedException e) {
            log.error("Trigger ARM RPO search thread sleep interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

}
