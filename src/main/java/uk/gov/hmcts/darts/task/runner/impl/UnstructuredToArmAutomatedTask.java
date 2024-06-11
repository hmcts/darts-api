package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.arm.component.AutomatedTaskProcessorFactory;
import uk.gov.hmcts.darts.arm.service.UnstructuredToArmProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.UNSTRUCTURED_TO_ARM_TASK_NAME;

@Slf4j
public class UnstructuredToArmAutomatedTask extends AbstractLockableAutomatedTask {

    protected String taskName = UNSTRUCTURED_TO_ARM_TASK_NAME.getTaskName();
    private final AutomatedTaskProcessorFactory automatedTaskProcessorFactory;

    public UnstructuredToArmAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                          LockProvider lockProvider,
                                          AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                          AutomatedTaskProcessorFactory taskProcessorFactory,
                                          LogApi logApi) {
        super(automatedTaskRepository, lockProvider, automatedTaskConfigurationProperties, logApi);
        this.automatedTaskProcessorFactory = taskProcessorFactory;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    protected void runTask() {
        Integer batchSize = getAutomatedTaskBatchSize(taskName);
        UnstructuredToArmProcessor unstructuredToArmProcessor = automatedTaskProcessorFactory.createUnstructuredToArmProcessor(batchSize);
        unstructuredToArmProcessor.processUnstructuredToArm();
    }

    @SuppressWarnings("java:S4507")
    @Override
    protected void handleException(Exception exception) {
        log.error("{} Exception", getTaskName(), exception);
    }
}
