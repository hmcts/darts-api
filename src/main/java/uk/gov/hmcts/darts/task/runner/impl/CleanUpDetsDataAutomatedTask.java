package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.service.CleanupDetsDataService;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.CleanupDetsDataAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.CLEANUP_DETS_DATA;

@Slf4j
@Component
public class CleanUpDetsDataAutomatedTask extends AbstractLockableAutomatedTask<CleanupDetsDataAutomatedTaskConfig>
    implements AutoloadingManualTask {

    private final CleanupDetsDataService cleanupDetsDataService;
    private final CleanupDetsDataAutomatedTaskConfig cleanupDetsDataAutomatedTaskConfig;

    public CleanUpDetsDataAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                        CleanupDetsDataAutomatedTaskConfig cleanupDetsDataAutomatedTaskConfig,
                                        CleanupDetsDataService cleanupDetsDataService, LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, cleanupDetsDataAutomatedTaskConfig, logApi, lockService);
        this.cleanupDetsDataService = cleanupDetsDataService;
        this.cleanupDetsDataAutomatedTaskConfig = cleanupDetsDataAutomatedTaskConfig;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return CLEANUP_DETS_DATA;
    }

    @Override
    protected void runTask() {
        cleanupDetsDataService.cleanupDetsData(getAutomatedTaskBatchSize(), cleanupDetsDataAutomatedTaskConfig.getDurationInArmStorage());
    }

}
