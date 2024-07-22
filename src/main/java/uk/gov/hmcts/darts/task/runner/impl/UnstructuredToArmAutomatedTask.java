package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.arm.component.AutomatedTaskProcessorFactory;
import uk.gov.hmcts.darts.arm.service.UnstructuredToArmProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.UNSTRUCTURED_TO_ARM_TASK_NAME;

@Slf4j
public class UnstructuredToArmAutomatedTask extends AbstractLockableAutomatedTask {

    protected String taskName = UNSTRUCTURED_TO_ARM_TASK_NAME.getTaskName();
    private final AutomatedTaskProcessorFactory automatedTaskProcessorFactory;

    public UnstructuredToArmAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                          AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                          AutomatedTaskProcessorFactory taskProcessorFactory,
                                          LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
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
}
