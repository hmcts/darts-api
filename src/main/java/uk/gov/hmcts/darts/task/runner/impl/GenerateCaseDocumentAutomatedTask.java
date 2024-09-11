package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.component.AutomatedTaskProcessorFactory;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.GENERATE_CASE_DOCUMENT_TASK_NAME;

@Slf4j
@Component
@SuppressWarnings({"squid:S1135"})
public class GenerateCaseDocumentAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {

    protected String taskName = GENERATE_CASE_DOCUMENT_TASK_NAME.getTaskName();
    private final AutomatedTaskProcessorFactory automatedTaskProcessorFactory;

    public GenerateCaseDocumentAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                             AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                             AutomatedTaskProcessorFactory automatedTaskProcessorFactory,
                                             LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.automatedTaskProcessorFactory = automatedTaskProcessorFactory;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    protected void runTask() {
        Integer batchSize = getAutomatedTaskBatchSize(taskName);
        GenerateCaseDocumentProcessor processor = automatedTaskProcessorFactory.createGenerateCaseDocumentProcessor(batchSize);
        processor.processGenerateCaseDocument();
    }

}
