package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.cases.service.CaseExpiryDeleter;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.CaseExpiryDeletionAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

@Component
@ConditionalOnProperty(
    value = "darts.automated.task.case-expiry-deletion.enabled",
    havingValue = "true"
)
@Slf4j
public class CaseExpiryDeletionAutomatedTask
    extends AbstractLockableAutomatedTask<CaseExpiryDeletionAutomatedTaskConfig>
    implements AutoloadingManualTask {

    private final CaseExpiryDeleter caseExpiryDeleter;

    public CaseExpiryDeletionAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                           CaseExpiryDeletionAutomatedTaskConfig automatedTaskConfigurationProperties,
                                           LogApi logApi, LockService lockService,
                                           CaseExpiryDeleter caseExpiryDeleter) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.caseExpiryDeleter = caseExpiryDeleter;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return AutomatedTaskName.CASE_EXPIRY_DELETION_TASK_NAME;
    }

    @Override
    public void runTask() {
        caseExpiryDeleter.delete(getAutomatedTaskBatchSize());
    }

}
