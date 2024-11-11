package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.retention.service.ApplyRetentionCaseAssociatedObjectsProcessor;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.ApplyRetentionCaseAssociatedObjectsAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.APPLY_RETENTION_CASE_ASSOCIATED_OBJECTS_TASK_NAME;

@Slf4j
@Component
@SuppressWarnings({"squid:S1135"})
public class ApplyRetentionCaseAssociatedObjectsAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {

    private final ApplyRetentionCaseAssociatedObjectsProcessor processor;

    @Autowired
    public ApplyRetentionCaseAssociatedObjectsAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                            ApplyRetentionCaseAssociatedObjectsAutomatedTaskConfig automatedTaskConfigurationProperties,
                                                            ApplyRetentionCaseAssociatedObjectsProcessor processor,
                                                            LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.processor = processor;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return APPLY_RETENTION_CASE_ASSOCIATED_OBJECTS_TASK_NAME;
    }

    @Override
    protected void runTask() {
        processor.processApplyRetentionToCaseAssociatedObjects(getAutomatedTaskBatchSize());
    }
}
