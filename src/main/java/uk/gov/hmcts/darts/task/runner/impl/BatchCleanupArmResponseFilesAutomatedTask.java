package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.service.BatchCleanupArmResponseFilesService;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.BATCH_CLEANUP_ARM_RESPONSE_FILES_TASK_NAME;

@Slf4j
@Component
public class BatchCleanupArmResponseFilesAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {
    protected String taskName = BATCH_CLEANUP_ARM_RESPONSE_FILES_TASK_NAME.getTaskName();
    private final BatchCleanupArmResponseFilesService batchCleanupArmResponseFilesService;

    public BatchCleanupArmResponseFilesAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                     AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                                     BatchCleanupArmResponseFilesService batchCleanupArmResponseFilesService,
                                                     LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.batchCleanupArmResponseFilesService = batchCleanupArmResponseFilesService;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    protected void runTask() {
        Integer batchSize = getAutomatedTaskBatchSize(taskName);
        batchCleanupArmResponseFilesService.cleanupResponseFiles(batchSize);
    }
}
