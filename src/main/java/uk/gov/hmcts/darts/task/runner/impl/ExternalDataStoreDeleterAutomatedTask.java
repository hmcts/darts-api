package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.audio.service.ExternalDataStoreDeleter;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.EXTERNAL_DATASTORE_DELETER;

@Slf4j
public class ExternalDataStoreDeleterAutomatedTask extends AbstractLockableAutomatedTask {
    private final ExternalDataStoreDeleter externalDataStoreDeleter;
    protected String taskName = EXTERNAL_DATASTORE_DELETER.getTaskName();

    public ExternalDataStoreDeleterAutomatedTask(AutomatedTaskRepository automatedTaskRepository, LockProvider lockProvider,
                                                 AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                                 ExternalDataStoreDeleter deleter) {
        super(automatedTaskRepository, lockProvider, automatedTaskConfigurationProperties);
        this.externalDataStoreDeleter = deleter;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    protected void runTask() {
        externalDataStoreDeleter.delete();
    }

    @Override
    protected void handleException(Exception exception) {
        log.error("Exception: {}", exception.getMessage());
    }
}
