package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.service.ArmRpoBacklogCatchupService;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.ArmRpoBacklogCatchupAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.ARM_RPO_BACKLOG_CATCHUP;

@Slf4j
@Component
public class ArmRpoBacklogCatchupAutomatedTask
    extends AbstractLockableAutomatedTask<ArmRpoBacklogCatchupAutomatedTaskConfig>
    implements AutoloadingManualTask {

    private final ArmRpoBacklogCatchupService armRpoBacklogCatchupService;

    @Autowired
    public ArmRpoBacklogCatchupAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                             ArmRpoBacklogCatchupAutomatedTaskConfig armRpoBacklogCatchupAutomatedTaskConfig,
                                             LogApi logApi, LockService lockService,
                                             ArmRpoBacklogCatchupService armRpoBacklogCatchupService) {
        super(automatedTaskRepository, armRpoBacklogCatchupAutomatedTaskConfig, logApi, lockService);
        this.armRpoBacklogCatchupService = armRpoBacklogCatchupService;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return ARM_RPO_BACKLOG_CATCHUP;
    }

    @Override
    protected void runTask() {
        armRpoBacklogCatchupService.performCatchup(getAutomatedTaskBatchSize(),
                                                   getConfig().getMaxHoursEndingPoint(),
                                                   getConfig().getTotalCatchupHours(),
                                                   getConfig().getThreadSleepDuration());
    }
}
