package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.arm.component.AutomatedTaskProcessorFactory;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.GENERATE_CASE_DOCUMENT_TASK_NAME;

@Slf4j
@SuppressWarnings({"squid:S1135"})
public class GenerateCaseDocumentAutomatedTask extends AbstractLockableAutomatedTask {

    protected String taskName = GENERATE_CASE_DOCUMENT_TASK_NAME.getTaskName();
    private final AutomatedTaskProcessorFactory automatedTaskProcessorFactory;

    public GenerateCaseDocumentAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                             LockProvider lockProvider,
                                             AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                             AutomatedTaskProcessorFactory automatedTaskProcessorFactory,
                                             LogApi logApi) {
        super(automatedTaskRepository, lockProvider, automatedTaskConfigurationProperties, logApi);
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

    @Override
    protected void handleException(Exception exception) {
        log.error("Exception: {}", exception.getMessage());
    }

}
