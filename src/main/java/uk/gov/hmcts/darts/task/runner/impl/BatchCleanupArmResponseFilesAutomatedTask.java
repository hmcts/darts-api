package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.arm.service.BatchCleanupArmResponseFilesService;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.BATCH_CLEANUP_ARM_RESPONSE_FILES_TASK_NAME;

@Slf4j
public class BatchCleanupArmResponseFilesAutomatedTask extends AbstractLockableAutomatedTask {
    protected String taskName = BATCH_CLEANUP_ARM_RESPONSE_FILES_TASK_NAME.getTaskName();
    private final BatchCleanupArmResponseFilesService batchCleanupArmResponseFilesService;

    public BatchCleanupArmResponseFilesAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                     LockProvider lockProvider,
                                                     AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                                     BatchCleanupArmResponseFilesService batchCleanupArmResponseFilesService, LogApi logApi) {
        super(automatedTaskRepository, lockProvider, automatedTaskConfigurationProperties, logApi);
        this.batchCleanupArmResponseFilesService = batchCleanupArmResponseFilesService;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    protected void runTask() {
        Integer batchSize = getAutomatedTaskBatchSize(taskName);
        if (batchSize == 0) {
            batchSize = 1000000;
        }

        batchCleanupArmResponseFilesService.cleanupResponseFiles(batchSize);
    }

    @Override
    protected void handleException(Exception exception) {
        log.error("Exception: {}", exception.getMessage());
    }
}
