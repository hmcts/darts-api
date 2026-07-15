package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.retention.service.ResetRetentionAfterDocumentChangeService;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.ResetRetentionAfterDocumentChangeAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

@Slf4j
@Component
public class ResetRetentionAfterDocumentChangeAutomatedTask extends AbstractLockableAutomatedTask<ResetRetentionAfterDocumentChangeAutomatedTaskConfig>
    implements AutoloadingManualTask {

    private ResetRetentionAfterDocumentChangeService resetRetentionAfterDocumentChangeService;

    public ResetRetentionAfterDocumentChangeAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                          ResetRetentionAfterDocumentChangeService resetRetentionAfterDocumentChangeService,
                                                          ResetRetentionAfterDocumentChangeAutomatedTaskConfig resetRetentionAfterDocumentChangeAutomatedTaskConfig,
                                                          LogApi logApi,
                                                          LockService lockService) {
        super(automatedTaskRepository, resetRetentionAfterDocumentChangeAutomatedTaskConfig, logApi, lockService);
        this.resetRetentionAfterDocumentChangeService = resetRetentionAfterDocumentChangeService;
    }

    @Override
    protected void runTask() {
        resetRetentionAfterDocumentChangeService.updateRetentionAfterDocumentChange(getAutomatedTaskBatchSize());
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return AutomatedTaskName.RESET_RETENTION_AFTER_DOCUMENT_CHANGE;
    }
}
