package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.arm.service.ArmResponseFilesProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.PROCESS_ARM_RESPONSE_FILES_TASK_NAME;

@Slf4j
public class ProcessArmResponseFilesAutomatedTask extends AbstractLockableAutomatedTask {

    private final ArmResponseFilesProcessor armResponseFilesProcessor;
    protected String taskName = PROCESS_ARM_RESPONSE_FILES_TASK_NAME.getTaskName();

    public ProcessArmResponseFilesAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
          LockProvider lockProvider,
          AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
          ArmResponseFilesProcessor armResponseFilesProcessor) {
        super(automatedTaskRepository, lockProvider, automatedTaskConfigurationProperties);
        this.armResponseFilesProcessor = armResponseFilesProcessor;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    protected void runTask() {
        armResponseFilesProcessor.processResponseFiles();
    }

    @Override
    protected void handleException(Exception exception) {
        log.error("Exception: {}", exception.getMessage());
    }
}
