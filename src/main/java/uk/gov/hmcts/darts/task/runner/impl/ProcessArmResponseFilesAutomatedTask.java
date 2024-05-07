package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.arm.component.AutomatedTaskProcessorFactory;
import uk.gov.hmcts.darts.arm.service.ArmResponseFilesProcessor;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import java.util.Optional;

import static java.util.Objects.nonNull;
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
        Optional<AutomatedTaskEntity> automatedTaskEntity = getAutomatedTaskDetails(taskName);

        if (automatedTaskEntity.isEmpty()) {
            handleException(new Exception("Unable to find automated task details"));
            return;
        }

        AutomatedTaskEntity automatedTask = automatedTaskEntity.get();
        boolean isBatchMode = nonNull(automatedTask.getBatchSize()) && automatedTask.getBatchSize() > 0;

        ArmResponseFilesProcessor armResponseFilesProcessor = automatedTaskProcessorFactory.createArmResponseFilesProcessor(isBatchMode);
        armResponseFilesProcessor.processResponseFiles();
    }

    @Override
    protected void handleException(Exception exception) {
        log.error("Exception ", exception);
    }
}
