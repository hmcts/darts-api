package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.arm.component.AutomatedTaskProcessorFactory;
import uk.gov.hmcts.darts.arm.service.ArmResponseFilesProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.PROCESS_ARM_RESPONSE_FILES_TASK_NAME;

@Slf4j
public class ProcessArmResponseFilesAutomatedTask extends AbstractLockableAutomatedTask {
    protected String taskName = PROCESS_ARM_RESPONSE_FILES_TASK_NAME.getTaskName();
    private final AutomatedTaskProcessorFactory automatedTaskProcessorFactory;

    public ProcessArmResponseFilesAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                LockProvider lockProvider,
                                                AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                                AutomatedTaskProcessorFactory automatedTaskProcessorFactory,
                                                LogApi logApi) {
        super(automatedTaskRepository, lockProvider, automatedTaskConfigurationProperties, logApi);
        this.automatedTaskProcessorFactory = automatedTaskProcessorFactory;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    protected void runTask() {
        boolean inBatchMode = isAutomatedTaskInBatchMode(taskName);

        ArmResponseFilesProcessor armResponseFilesProcessor = automatedTaskProcessorFactory.createArmResponseFilesProcessor(inBatchMode);
        armResponseFilesProcessor.processResponseFiles();
    }

    @Override
    protected void handleException(Exception exception) {
        log.error("Exception ", exception);
    }
}
