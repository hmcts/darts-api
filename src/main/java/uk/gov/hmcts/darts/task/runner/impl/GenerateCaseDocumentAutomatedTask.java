package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.component.AutomatedTaskProcessorFactory;
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
@SuppressWarnings({"squid:S1135"})
public class GenerateCaseDocumentAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {

    private final AutomatedTaskProcessorFactory automatedTaskProcessorFactory;

    @Autowired
    public GenerateCaseDocumentAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                             GenerateCaseDocumentAutomatedTaskConfig automatedTaskConfigurationProperties,
                                             AutomatedTaskProcessorFactory automatedTaskProcessorFactory,
                                             LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.automatedTaskProcessorFactory = automatedTaskProcessorFactory;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return GENERATE_CASE_DOCUMENT_TASK_NAME;
    }

    @Override
    protected void runTask() {
        GenerateCaseDocumentProcessor processor = automatedTaskProcessorFactory.createGenerateCaseDocumentProcessor(getAutomatedTaskBatchSize());
        processor.processGenerateCaseDocument();
    }
}
