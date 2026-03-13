package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.service.CleanUpDetsDataProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.CleanUpDetsDataAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.CLEAN_UP_DETS_DATA;

@Slf4j
@Component
public class CleanUpDetsDataAutomatedTask
    extends AbstractLockableAutomatedTask<CleanUpDetsDataAutomatedTaskConfig>
    implements AutoloadingManualTask {

    private final CleanUpDetsDataProcessor cleanUpDetsDataProcessor;

    @Autowired
    public CleanUpDetsDataAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                        CleanUpDetsDataAutomatedTaskConfig automatedTaskConfigurationProperties,
                                        CleanUpDetsDataProcessor processor,
                                        LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.cleanUpDetsDataProcessor = processor;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return CLEAN_UP_DETS_DATA;
    }

    @Override
    protected void runTask() {
        cleanUpDetsDataProcessor.processCleanUpDetsData(getAutomatedTaskBatchSize(), getConfig());
    }
}