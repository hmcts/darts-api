package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.audio.service.OutboundDataStoreSoftDeleter;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.OUTBOUND_AUDIO_DELETER_TASK_NAME;

@Slf4j
public class OutboundDataStoreSoftDeleterAutomatedTask extends AbstractLockableAutomatedTask {

    protected String taskName = OUTBOUND_AUDIO_DELETER_TASK_NAME.getTaskName();
    private final OutboundDataStoreSoftDeleter outboundDataStoreSoftDeleter;

    public OutboundDataStoreSoftDeleterAutomatedTask(AutomatedTaskRepository automatedTaskRepository, LockProvider lockProvider,
                                                     AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                                     OutboundDataStoreSoftDeleter processor) {
        super(automatedTaskRepository, lockProvider, automatedTaskConfigurationProperties);
        this.outboundDataStoreSoftDeleter = processor;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    protected void runTask() {
        outboundDataStoreSoftDeleter.markForDeletion();
    }

    @Override
    protected void handleException(Exception exception) {
        log.error("Exception: {}", exception.getMessage());
    }
}
