package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentForRetentionDateProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.GenerateCaseDocumentForRetentionDateAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.GENERATE_CASE_DOCUMENT_FOR_RETENTION_DATE_TASK_NAME;

@Slf4j
@Component
public class GenerateCaseDocumentForRetentionDateAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {


    private final GenerateCaseDocumentForRetentionDateProcessor generateCaseDocumentForRetentionDateBatchProcessor;

    @Autowired
    public GenerateCaseDocumentForRetentionDateAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                             GenerateCaseDocumentForRetentionDateAutomatedTaskConfig automatedTaskConfigurationProperties,
                                                             LogApi logApi,
                                                             LockService lockService,
                                                             GenerateCaseDocumentForRetentionDateProcessor generateCaseDocumentForRetentionDateBatchProcessor) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.generateCaseDocumentForRetentionDateBatchProcessor = generateCaseDocumentForRetentionDateBatchProcessor;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return GENERATE_CASE_DOCUMENT_FOR_RETENTION_DATE_TASK_NAME;
    }

    @Override
    protected void runTask() {
        generateCaseDocumentForRetentionDateBatchProcessor.processGenerateCaseDocumentForRetentionDate(getAutomatedTaskBatchSize());
    }
}
