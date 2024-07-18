package uk.gov.hmcts.darts.task.runner.impl;

import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.arm.component.AutomatedTaskProcessorFactory;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.event.service.CleanupCurrentFlagEventProcessor;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.EVENT_CLEANUP_CURRENT_TASK;

public class CleanupCurrentEventTask extends AbstractLockableAutomatedTask {

    protected String taskName = EVENT_CLEANUP_CURRENT_TASK.getTaskName();
    private AutomatedTaskProcessorFactory automatedTaskProcessorFactory;

    public CleanupCurrentEventTask(AutomatedTaskRepository automatedTaskRepository,
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
        Integer batchSize = getAutomatedTaskBatchSize(taskName);
        CleanupCurrentFlagEventProcessor processor = automatedTaskProcessorFactory.createCleanupCurrentFlagEventProcessor(batchSize);
        processor.processCurrentEvent();
    }
}