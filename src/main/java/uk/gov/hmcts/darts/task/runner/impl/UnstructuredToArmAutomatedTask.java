package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.arm.service.UnstructuredToArmProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.UNSTRUCTURED_TO_ARM_TASK_NAME;

@Slf4j
public class UnstructuredToArmAutomatedTask extends AbstractLockableAutomatedTask {

    protected String taskName = UNSTRUCTURED_TO_ARM_TASK_NAME.getTaskName();
    private final UnstructuredToArmProcessor unstructuredToArmProcessor;

    public UnstructuredToArmAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                              LockProvider lockProvider,
                                              AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                              UnstructuredToArmProcessor processor) {
        super(automatedTaskRepository, lockProvider, automatedTaskConfigurationProperties);
        this.unstructuredToArmProcessor = processor;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    protected void runTask() {
        unstructuredToArmProcessor.processUnstructuredToArm();
    }

    @SuppressWarnings("java:S4507")
    @Override
    protected void handleException(Exception exception) {
        if (exception instanceof NullPointerException npe) {
            log.error("Exception with null: {}", npe.getMessage(), npe);
        }
        log.error("Exception: {}", exception.getMessage());
    }
}
