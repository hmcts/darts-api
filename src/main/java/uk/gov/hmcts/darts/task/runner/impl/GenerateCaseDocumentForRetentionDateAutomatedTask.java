package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.component.AutomatedTaskProcessorFactory;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentForRetentionDateProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import java.time.Duration;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.GENERATE_CASE_DOCUMENT_FOR_RETENTION_DATE_TASK_NAME;

@Slf4j
@Component
@SuppressWarnings({"squid:S1135"})
public class GenerateCaseDocumentForRetentionDateAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {

    private final AutomatedTaskProcessorFactory automatedTaskProcessorFactory;

    @Autowired
    public GenerateCaseDocumentForRetentionDateAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                             AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                                             AutomatedTaskProcessorFactory automatedTaskProcessorFactory,
                                                             LogApi logApi,
                                                             LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.automatedTaskProcessorFactory = automatedTaskProcessorFactory;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return GENERATE_CASE_DOCUMENT_FOR_RETENTION_DATE_TASK_NAME;
    }

    @Override
    public Duration getLockAtMostFor() {
        return Duration.ofMinutes(40);
    }

    @Override
    protected void runTask() {
        Integer batchSize = getAutomatedTaskBatchSize();
        GenerateCaseDocumentForRetentionDateProcessor processor = automatedTaskProcessorFactory.createGenerateCaseDocumentForRetentionDateProcessor(batchSize);
        processor.processGenerateCaseDocumentForRetentionDate(batchSize);
    }
}
