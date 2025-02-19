package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.cases.service.CloseOldCasesProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.CloseOldCasesAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.CLOSE_OLD_CASES_TASK_NAME;

@Slf4j
@Component
public class CloseOldCasesAutomatedTask 
    extends AbstractLockableAutomatedTask<CloseOldCasesAutomatedTaskConfig>
    implements AutoloadingManualTask {

    private final CloseOldCasesProcessor closeOldCasesProcessor;

    @Autowired
    public CloseOldCasesAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                      CloseOldCasesAutomatedTaskConfig automatedTaskConfigurationProperties,
                                      LogApi logApi, LockService lockService,
                                      CloseOldCasesProcessor closeOldCasesProcessor) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.closeOldCasesProcessor = closeOldCasesProcessor;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return CLOSE_OLD_CASES_TASK_NAME;
    }

    @Override
    protected void runTask() {
        closeOldCasesProcessor.closeCases(getAutomatedTaskBatchSize());
    }
}
