package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.deleter.impl.inbound.ExternalInboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.outbound.ExternalOutboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.unstructured.ExternalUnstructuredDataStoreDeleter;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import java.time.Duration;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.EXTERNAL_DATASTORE_DELETER_TASK_NAME;

@Slf4j
@Component
public class ExternalDataStoreDeleterAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {

    private final ExternalInboundDataStoreDeleter inboundDeleter;
    private final ExternalUnstructuredDataStoreDeleter unstructuredDeleter;
    private final ExternalOutboundDataStoreDeleter outboundDeleter;


    @Autowired
    public ExternalDataStoreDeleterAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                 AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                                 ExternalInboundDataStoreDeleter inboundDeleter,
                                                 ExternalUnstructuredDataStoreDeleter unstructuredDeleter,
                                                 ExternalOutboundDataStoreDeleter outboundDeleter,
                                                 LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.inboundDeleter = inboundDeleter;
        this.unstructuredDeleter = unstructuredDeleter;
        this.outboundDeleter = outboundDeleter;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return EXTERNAL_DATASTORE_DELETER_TASK_NAME;
    }

    @Override
    public Duration getLockAtMostFor() {
        return Duration.ofMinutes(90);
    }

    @Override
    protected void runTask() {
        Integer batchSize = getAutomatedTaskBatchSize();
        inboundDeleter.delete(batchSize);
        unstructuredDeleter.delete(batchSize);
        outboundDeleter.delete(batchSize);
    }
}
