package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.arm.service.ArmBatchProcessResponseFiles;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.BATCH_PROCESS_ARM_RESPONSE_FILES_TASK_NAME;

@Slf4j
public class BatchProcessArmResponseFilesAutomatedTask extends AbstractLockableAutomatedTask {

    private String taskName = BATCH_PROCESS_ARM_RESPONSE_FILES_TASK_NAME.getTaskName();

    private final ArmBatchProcessResponseFiles armBatchProcessResponseFiles;

    public BatchProcessArmResponseFilesAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                     LockProvider lockProvider,
                                                     AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                                     ArmBatchProcessResponseFiles armBatchProcessResponseFiles) {
        super(automatedTaskRepository, lockProvider, automatedTaskConfigurationProperties);
        this.armBatchProcessResponseFiles = armBatchProcessResponseFiles;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    protected void runTask() {
        armBatchProcessResponseFiles.batchProcessResponseFiles();
    }

    @Override
    protected void handleException(Exception exception) {
        log.error("Exception ", exception);
    }
}
