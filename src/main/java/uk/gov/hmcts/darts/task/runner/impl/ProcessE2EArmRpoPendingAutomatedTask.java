package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.service.TriggerArmRpoSearchService;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.ProcessE2EArmRpoPendingAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

@ConditionalOnProperty(
    value = "darts.automated.task.process-e2e-arm-rpo-pending.process-e2e-arm-rpo",
    havingValue = "true"
)
@Slf4j
@Component
public class ProcessE2EArmRpoPendingAutomatedTask
    extends AbstractLockableAutomatedTask<ProcessE2EArmRpoPendingAutomatedTaskConfig>
    implements AutoloadingManualTask {

    private final TriggerArmRpoSearchService triggerArmRpoSearchService;

    protected ProcessE2EArmRpoPendingAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                   ProcessE2EArmRpoPendingAutomatedTaskConfig configurationProperties,
                                                   LogApi logApi,
                                                   LockService lockService,
                                                   TriggerArmRpoSearchService triggerArmRpoSearchService) {
        super(automatedTaskRepository, configurationProperties, logApi, lockService);
        this.triggerArmRpoSearchService = triggerArmRpoSearchService;
    }

    @Override
    protected void runTask() {
        triggerArmRpoSearchService.triggerArmRpoSearch();
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return AutomatedTaskName.PROCESS_E2E_ARM_PENDING_TASK_NAME;
    }

}
