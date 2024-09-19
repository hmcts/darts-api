package uk.gov.hmcts.darts.task.runner.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.component.AutomatedTaskProcessorFactory;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.event.service.CleanupCurrentFlagEventProcessor;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.EVENT_CLEANUP_CURRENT_TASK;

@Component
public class CleanupCurrentEventTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {

    private final AutomatedTaskProcessorFactory automatedTaskProcessorFactory;

    @Autowired
    public CleanupCurrentEventTask(AutomatedTaskRepository automatedTaskRepository,
                                   AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                   AutomatedTaskProcessorFactory automatedTaskProcessorFactory,
                                   LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.automatedTaskProcessorFactory = automatedTaskProcessorFactory;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return EVENT_CLEANUP_CURRENT_TASK;
    }

    @Override
    protected void runTask() {
        Integer batchSize = getAutomatedTaskBatchSize(getTaskName());
        CleanupCurrentFlagEventProcessor processor = automatedTaskProcessorFactory.createCleanupCurrentFlagEventProcessor(batchSize);
        processor.processCurrentEvent();
    }
}