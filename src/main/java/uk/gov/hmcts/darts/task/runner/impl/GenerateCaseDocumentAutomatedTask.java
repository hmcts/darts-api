package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.GenerateCaseDocumentAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.GENERATE_CASE_DOCUMENT_TASK_NAME;

@Slf4j
@Component
public class GenerateCaseDocumentAutomatedTask 
    extends AbstractLockableAutomatedTask<GenerateCaseDocumentAutomatedTaskConfig>
    implements AutoloadingManualTask {

    private final GenerateCaseDocumentProcessor generateCaseDocumentBatchProcessor;

    @Autowired
    public GenerateCaseDocumentAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                             GenerateCaseDocumentAutomatedTaskConfig automatedTaskConfigurationProperties,
                                             LogApi logApi, LockService lockService,
                                             GenerateCaseDocumentProcessor generateCaseDocumentBatchProcessor) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.generateCaseDocumentBatchProcessor = generateCaseDocumentBatchProcessor;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return GENERATE_CASE_DOCUMENT_TASK_NAME;
    }

    @Override
    protected void runTask() {
        generateCaseDocumentBatchProcessor.processGenerateCaseDocument(getAutomatedTaskBatchSize());
    }
}
