package uk.gov.hmcts.darts.task.runner.impl;

import uk.gov.hmcts.darts.arm.component.AutomatedTaskProcessorFactory;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.event.service.CleanupCurrentFlagEventProcessor;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.EVENT_CLEANUP_CURRENT_TASK;

public class CleanupCurrentEventTask extends AbstractLockableAutomatedTask {

    protected String taskName = EVENT_CLEANUP_CURRENT_TASK.getTaskName();
    private AutomatedTaskProcessorFactory automatedTaskProcessorFactory;

    public CleanupCurrentEventTask(AutomatedTaskRepository automatedTaskRepository,
                                   AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                   AutomatedTaskProcessorFactory automatedTaskProcessorFactory,
                                   LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi,lockService);
        this.automatedTaskProcessorFactory = automatedTaskProcessorFactory;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    protected void runTask() {
        Integer batchSize = getAutomatedTaskBatchSize(taskName);
        CleanupCurrentFlagEventProcessor processor = automatedTaskProcessorFactory.createCleanupCurrentFlagEventProcessor(batchSize);
        processor.processCurrentEvent();
    }
}