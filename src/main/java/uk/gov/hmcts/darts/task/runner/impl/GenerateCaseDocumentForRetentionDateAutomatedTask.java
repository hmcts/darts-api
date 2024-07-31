package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.arm.component.AutomatedTaskProcessorFactory;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentForRetentionDateProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.GENERATE_CASE_DOCUMENT_FOR_RETENTION_DATE_TASK_NAME;

@Slf4j
@SuppressWarnings({"squid:S1135"})
public class GenerateCaseDocumentForRetentionDateAutomatedTask extends AbstractLockableAutomatedTask {

    protected String taskName = GENERATE_CASE_DOCUMENT_FOR_RETENTION_DATE_TASK_NAME.getTaskName();
    private final AutomatedTaskProcessorFactory automatedTaskProcessorFactory;

    public GenerateCaseDocumentForRetentionDateAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
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
        GenerateCaseDocumentForRetentionDateProcessor processor = automatedTaskProcessorFactory.createGenerateCaseDocumentForRetentionDate(batchSize);
        processor.processGenerateCaseDocumentForRetentionDate();
    }
}
