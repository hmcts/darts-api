package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.retention.service.ApplyRetentionCaseAssociatedObjectsProcessor;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.APPLY_RETENTION_CASE_ASSOCIATED_OBJECTS_TASK_NAME;

@Slf4j
@SuppressWarnings({"squid:S1135"})
public class ApplyRetentionCaseAssociatedObjectsAutomatedTask extends AbstractLockableAutomatedTask {

    protected String taskName = APPLY_RETENTION_CASE_ASSOCIATED_OBJECTS_TASK_NAME.getTaskName();
    private final ApplyRetentionCaseAssociatedObjectsProcessor processor;

    public ApplyRetentionCaseAssociatedObjectsAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                            LockProvider lockProvider,
                                                            AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                                            ApplyRetentionCaseAssociatedObjectsProcessor processor,
                                                            LogApi logApi) {
        super(automatedTaskRepository, lockProvider, automatedTaskConfigurationProperties, logApi);
        this.processor = processor;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    protected void runTask() {
        processor.processApplyRetentionToCaseAssociatedObjects();
    }
}
