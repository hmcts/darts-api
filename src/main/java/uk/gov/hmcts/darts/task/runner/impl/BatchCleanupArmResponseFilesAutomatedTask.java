package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.service.BatchCleanupArmResponseFilesService;
import uk.gov.hmcts.darts.arm.service.impl.DartsBatchCleanupArmResponseFilesServiceImpl;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.DartsBatchCleanupArmResponseFilesAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.BATCH_CLEANUP_ARM_RESPONSE_FILES_TASK_NAME;

@Slf4j
@Component
public class BatchCleanupArmResponseFilesAutomatedTask 
    extends AbstractLockableAutomatedTask<DartsBatchCleanupArmResponseFilesAutomatedTaskConfig>
    implements AutoloadingManualTask {
    private final BatchCleanupArmResponseFilesService batchCleanupArmResponseFilesService;

    @Autowired
    public BatchCleanupArmResponseFilesAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                     DartsBatchCleanupArmResponseFilesAutomatedTaskConfig automatedTaskConfigurationProperties,
                                                     DartsBatchCleanupArmResponseFilesServiceImpl batchCleanupArmResponseFilesService,
                                                     LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.batchCleanupArmResponseFilesService = batchCleanupArmResponseFilesService;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return BATCH_CLEANUP_ARM_RESPONSE_FILES_TASK_NAME;
    }

    @Override
    protected void runTask() {
        batchCleanupArmResponseFilesService.cleanupResponseFiles(getAutomatedTaskBatchSize());
    }
}
