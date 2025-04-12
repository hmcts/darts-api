package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.datamanagement.service.AssociatedObjectDataExpiryDeleterService;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.AssociatedObjectDataExpiryDeletionAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

@Component
@Slf4j
public class AssociatedObjectDataExpiryDeletionAutomatedTask
    extends AbstractLockableAutomatedTask<AssociatedObjectDataExpiryDeletionAutomatedTaskConfig>
    implements AutoloadingManualTask {

    private final AssociatedObjectDataExpiryDeleterService associatedObjectDataExpiryDeleter;

    public AssociatedObjectDataExpiryDeletionAutomatedTask(
        AutomatedTaskRepository automatedTaskRepository,
        AssociatedObjectDataExpiryDeletionAutomatedTaskConfig automatedTaskConfigurationProperties,
        LogApi logApi, LockService lockService,
        AssociatedObjectDataExpiryDeleterService associatedObjectDataExpiryDeleter) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.associatedObjectDataExpiryDeleter = associatedObjectDataExpiryDeleter;
    }

    @Override
    public void run() {
        super.run();
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return AutomatedTaskName.ASSOCIATED_OBJECT_DATA_EXPIRY_DELETION_TASK_NAME;
    }

    @Override
    public void runTask() {
        associatedObjectDataExpiryDeleter.delete(getAutomatedTaskBatchSize());
    }

}
