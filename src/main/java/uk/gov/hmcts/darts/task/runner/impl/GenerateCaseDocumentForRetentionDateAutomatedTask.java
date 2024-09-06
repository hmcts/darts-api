package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.arm.component.AutomatedTaskProcessorFactory;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentForRetentionDateProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.GENERATE_CASE_DOCUMENT_FOR_RETENTION_DATE_TASK_NAME;

@Slf4j
@SuppressWarnings({"squid:S1135"})
public class GenerateCaseDocumentForRetentionDateAutomatedTask extends AbstractLockableAutomatedTask {

    protected String taskName = GENERATE_CASE_DOCUMENT_FOR_RETENTION_DATE_TASK_NAME.getTaskName();
    private final AutomatedTaskProcessorFactory automatedTaskProcessorFactory;

    public GenerateCaseDocumentForRetentionDateAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                             AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                                             AutomatedTaskProcessorFactory automatedTaskProcessorFactory,
                                                             LogApi logApi,
                                                             LockService lockService) {
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
        GenerateCaseDocumentForRetentionDateProcessor processor = automatedTaskProcessorFactory.createGenerateCaseDocumentForRetentionDateProcessor(batchSize);
        processor.processGenerateCaseDocumentForRetentionDate(batchSize);
    }
}
