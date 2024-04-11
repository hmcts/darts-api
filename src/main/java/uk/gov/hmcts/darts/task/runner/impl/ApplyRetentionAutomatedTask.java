package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.retention.service.ApplyRetentionProcessor;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.APPLY_RETENTION_TASK_NAME;

@Slf4j
public class ApplyRetentionAutomatedTask extends AbstractLockableAutomatedTask {

    protected String taskName = APPLY_RETENTION_TASK_NAME.getTaskName();
    ApplyRetentionProcessor applyRetentionProcessor;

    public ApplyRetentionAutomatedTask(AutomatedTaskRepository automatedTaskRepository, LockProvider lockProvider,
                                       AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                       ApplyRetentionProcessor applyRetentionProcessor, LogApi logApi) {
        super(automatedTaskRepository, lockProvider, automatedTaskConfigurationProperties, logApi);
        this.applyRetentionProcessor = applyRetentionProcessor;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    protected void runTask() {
        applyRetentionProcessor.processApplyRetention();
    }

    @Override
    protected void handleException(Exception exception) {
        log.error("Exception: {}", exception.getMessage());
    }
}
