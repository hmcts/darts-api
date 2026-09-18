package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.DisableInactiveUserAccountsAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;
import uk.gov.hmcts.darts.usermanagement.service.DisableInactiveUserAccountsService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.DISABLE_INACTIVE_USER_ACCOUNTS;

@Slf4j
@Component
public class DisableInactiveUserAccountsAutomatedTask
    extends AbstractLockableAutomatedTask<DisableInactiveUserAccountsAutomatedTaskConfig>
    implements AutoloadingManualTask {

    private final DisableInactiveUserAccountsService disableInactiveUserAccountsService;

    public DisableInactiveUserAccountsAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                    DisableInactiveUserAccountsAutomatedTaskConfig automatedTaskConfig,
                                                    LogApi logApi,
                                                    LockService lockService,
                                                    DisableInactiveUserAccountsService disableInactiveUserAccountsService) {
        super(automatedTaskRepository, automatedTaskConfig, logApi, lockService);
        this.disableInactiveUserAccountsService = disableInactiveUserAccountsService;
    }

    @Override
    protected void runTask() {
        disableInactiveUserAccountsService.process(getAutomatedTaskBatchSize());
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return DISABLE_INACTIVE_USER_ACCOUNTS;
    }
}
