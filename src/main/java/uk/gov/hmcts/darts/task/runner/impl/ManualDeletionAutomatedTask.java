package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;
import uk.gov.hmcts.darts.task.service.ManualDeletionProcessor;

import java.time.Duration;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.MANUAL_DELETION;

@Slf4j
@Component
@ConditionalOnProperty(
    value = "darts.manual-deletion.enabled",
    havingValue = "true"
)
public class ManualDeletionAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {

    private final ManualDeletionProcessor manualDeletionProcessor;


    @Autowired
    public ManualDeletionAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                       AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                       ManualDeletionProcessor manualDeletionProcessor,
                                       LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.manualDeletionProcessor = manualDeletionProcessor;

    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return MANUAL_DELETION;
    }

    @Override
    public Duration getLockAtMostFor() {
        return Duration.ofMinutes(90);
    }

    @Override
    protected void runTask() {
        manualDeletionProcessor.process(getAutomatedTaskBatchSize());
    }
}
